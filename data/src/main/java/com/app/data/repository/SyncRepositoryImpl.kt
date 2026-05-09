package com.app.data.repository

import android.util.Log
import com.app.domain.model.Note
import com.app.domain.model.SyncResult
import com.app.domain.util.effectiveUpdatedMillis
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository
import com.app.domain.repository.NoteRepository
import com.app.domain.repository.SyncRepository
import com.app.domain.repository.SyncScheduler

/**
 * Two-way synchroniser between the local Room database and Firestore.
 *
 * Algorithm (last-write-wins by `updatedAt`):
 *
 *  1. Fetch the full remote note list and a snapshot of the local note list.
 *  2. Index both sides by note id (== Firestore document id == local Room id).
 *  3. For every unique id, classify the row into one of:
 *       - **local-only**     → upload to Firestore.
 *       - **remote-only**    → insert/replace into Room (preserving id).
 *       - **both, local newer**  → upload local copy.
 *       - **both, remote newer** → overwrite local copy.
 *       - **both, equal**    → no-op (already converged).
 *  4. Push the upload set to Firestore one-by-one (per-note runCatching so a
 *     single failure doesn't abort the rest).
 *  5. Apply the remote-side deltas + flip the successfully-uploaded rows to
 *     `isSync = 1` inside a single Room transaction (see [NoteDao.applyMergeResult]).
 *  6. Stamp `lastSyncTime` on the user document and DataStore on success.
 *
 * Failures are absorbed and reported via [SyncResult]; the worker uses that
 * result to decide whether to retry, and the UI surfaces a friendly message.
 */
class SyncRepositoryImpl(
    private val noteRepository: NoteRepository,
    private val firestoreRepository: FirestoreRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) : SyncRepository {

    override suspend fun syncPendingNotes(userId: String): SyncResult {
        val remoteNotes = runCatching { firestoreRepository.fetchAllNotes(userId) }
            .getOrElse { error ->
                Log.w(TAG, "Failed to fetch remote notes", error)
                return SyncResult(
                    attempted = 0,
                    succeeded = 0,
                    failed = 0,
                    errorMessage = error.message ?: "Unable to reach Firestore"
                )
            }
        val localNotes = noteRepository.getAllNotesSnapshot()

        val plan = buildMergePlan(localNotes, remoteNotes)

        if (plan.toUpload.isEmpty() && plan.toDownload.isEmpty()) {
            // Both sides already converged — nothing to do, but still bump
            // lastSyncTime so the UI reflects a successful pass.
            stampLastSyncTime(userId)
            return SyncResult(0, 0, 0)
        }

        var succeeded = 0
        var failed = 0
        var lastError: String? = null

        val uploadedLocalIds = mutableListOf<Int>()
        for (note in plan.toUpload) {
            val localId = note.noteId?.toIntOrNull()
            if (localId == null) {
                failed++
                continue
            }
            val outcome = runCatching { firestoreRepository.uploadNote(userId, note) }
            if (outcome.isSuccess) {
                uploadedLocalIds += localId
                succeeded++
            } else {
                failed++
                lastError = outcome.exceptionOrNull()?.message
                Log.w(TAG, "Failed to upload note id=$localId", outcome.exceptionOrNull())
            }
        }

        // Apply the local side of the merge in a single Room transaction:
        // remote upserts + flipping just-uploaded rows to isSync = 1.
        val mergeOutcome = runCatching {
            noteRepository.applyMergeResult(
                remoteUpserts = plan.toDownload,
                uploadedLocalIds = uploadedLocalIds
            )
        }
        if (mergeOutcome.isSuccess) {
            succeeded += plan.toDownload.size
        } else {
            failed += plan.toDownload.size
            lastError = mergeOutcome.exceptionOrNull()?.message ?: lastError
            Log.w(TAG, "Failed to apply local merge", mergeOutcome.exceptionOrNull())
        }

        if (succeeded > 0) stampLastSyncTime(userId)

        return SyncResult(
            attempted = plan.toUpload.size + plan.toDownload.size,
            succeeded = succeeded,
            failed = failed,
            errorMessage = lastError.takeIf { failed > 0 }
        )
    }

    override suspend fun hasPendingNotes(): Boolean = noteRepository.hasPendingSyncNotes()

    override suspend fun onLocalNoteChanged() {
        // The DAO writes already set isSync = 0; we only need to nudge the
        // worker to upload soon (no-op if no user is signed in).
        if (authRepository.getCurrentUser() != null) {
            syncScheduler.requestImmediateSync()
        }
    }

    /**
     * Reconcile the two sides into deterministic upload/download buckets.
     *
     * Conflict keys use [com.app.domain.util.effectiveUpdatedMillis], which
     * prefers persisted millis columns and falls back to parsing legacy UI
     * timestamps. The larger value wins; equal timestamps are treated as already
     * converged and skipped.
     */
    private fun buildMergePlan(
        local: List<Note>,
        remote: List<Note>
    ): MergePlan {
        val localById = local
            .filter { !it.noteId.isNullOrBlank() }
            .associateBy { it.noteId!! }
        val remoteById = remote
            .filter { !it.noteId.isNullOrBlank() }
            .associateBy { it.noteId!! }

        val toUpload = mutableListOf<Note>()
        val toDownload = mutableListOf<Note>()

        for (id in localById.keys + remoteById.keys) {
            val localNote = localById[id]
            val remoteNote = remoteById[id]
            when {
                localNote != null && remoteNote == null -> toUpload += localNote
                localNote == null && remoteNote != null -> toDownload += remoteNote
                localNote != null && remoteNote != null -> {
                    val localTs = localNote.effectiveUpdatedMillis()
                    val remoteTs = remoteNote.effectiveUpdatedMillis()
                    when {
                        localTs > remoteTs -> toUpload += localNote
                        remoteTs > localTs -> toDownload += remoteNote
                        // Equal timestamps → already converged; skip.
                    }
                }
            }
        }

        return MergePlan(toUpload = toUpload, toDownload = toDownload)
    }

    private suspend fun stampLastSyncTime(userId: String) {
        val now = System.currentTimeMillis()
        runCatching { firestoreRepository.updateLastSyncTime(userId, now) }
            .onFailure { Log.w(TAG, "Failed to stamp lastSyncTime", it) }
    }

    private data class MergePlan(
        val toUpload: List<Note>,
        val toDownload: List<Note>
    )

    private companion object {
        const val TAG = "SyncRepositoryImpl"
    }
}
