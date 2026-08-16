package com.app.data.repository

import android.util.Log
import com.app.domain.model.Note
import com.app.domain.model.NoteSyncStatus
import com.app.domain.model.SyncResult
import com.app.domain.model.hasMeaningfulContent
import com.app.domain.util.effectiveUpdatedMillis
import com.app.domain.repository.FirestoreRepository
import com.app.domain.repository.NoteRepository
import com.app.domain.repository.SyncRepository

/**
 * Two-way synchroniser between the local Room database and Firestore.
 *
 * Triggered automatically after a successful Room save (create/edit/delete)
 * and whenever WorkManager has a network connection. Note creation and
 * edits always persist to Room first with a pending sync flag; this
 * repository never writes to Firestore unless the local row already exists.
 *
 * Algorithm (last-write-wins by `updatedAt`, with empty-remote protection):
 *
 *  1. Fetch the full remote note list. On failure, abort without touching
 *     local rows so valid Room data cannot be lost to a network error.
 *  2. Flush local tombstones: delete matching Firestore docs (or restore
 *     a newer remote copy), then purge Room.
 *  3. Snapshot active local notes and index both sides by note id
 *     (== Firestore document id == local Room id).
 *  4. For every unique id, classify the row into one of:
 *       - **local-only**          → upload to Firestore.
 *       - **remote-only**         → insert/replace into Room (preserving id),
 *                                   skipped when the remote payload is empty.
 *       - **both, local newer**   → upload local copy.
 *       - **both, remote newer**  → overwrite local copy unless remote is
 *                                   empty/null while local has real content.
 *       - **both, equal**         → no-op (already converged).
 *  5. Push the upload set to Firestore one-by-one using the existing note id
 *     (per-note runCatching so a single failure doesn't abort the rest).
 *  6. Apply remote-side deltas + flip just-uploaded rows to `SYNCED`
 *     in a single Room transaction.
 *  7. Stamp `lastSyncTime` on the user document and DataStore on a clean pass.
 *
 * Failures are absorbed and reported via [SyncResult]; the worker uses that
 * result to decide whether to retry. Local data is never discarded because
 * Firestore is unavailable.
 */
class SyncRepositoryImpl(
    private val noteRepository: NoteRepository,
    private val firestoreRepository: FirestoreRepository
) : SyncRepository {

    override suspend fun syncPendingNotes(userId: String): SyncResult {
        val remoteResult = runCatching { firestoreRepository.fetchAllNotes(userId) }
        val remoteNotes = remoteResult.getOrElse { error ->
            // Permission / network failures used to abort the whole pass before any
            // upload. Fall back to pushing local pending notes so create-while-signed-in
            // still reaches Firestore.
            Log.w(TAG, "Failed to fetch remote notes; uploading local pending only", error)
            emptyList()
        }
        if (remoteResult.isFailure) {
            return uploadLocalPendingOnly(userId)
        }

        val tombstoneResult = flushTombstones(userId, remoteNotes)
        val restoredIds = tombstoneResult.restoredRemoteIds.toSet()
        val deletedIds = tombstoneResult.deletedRemoteIds.toSet()
        // Any row still tombstoned must stay hidden — never re-download it.
        val pendingTombstoneIds = noteRepository.getTombstonedNotes()
            .mapNotNull { it.noteId }
            .toSet()
        val localNotes = noteRepository.getAllNotesSnapshot()
        val remoteForMerge = remoteNotes.filterNot { note ->
            val id = note.noteId
            id != null && (
                id in restoredIds ||
                    id in deletedIds ||
                    id in pendingTombstoneIds
                )
        }

        val plan = buildMergePlan(localNotes, remoteForMerge)

        if (plan.toUpload.isEmpty() && plan.toDownload.isEmpty()) {
            if (tombstoneResult.failed == 0) stampLastSyncTime(userId)
            return SyncResult(
                attempted = tombstoneResult.attempted,
                succeeded = tombstoneResult.succeeded,
                failed = tombstoneResult.failed,
                errorMessage = tombstoneResult.errorMessage
            )
        }

        var succeeded = tombstoneResult.succeeded
        var failed = tombstoneResult.failed
        var lastError: String? = tombstoneResult.errorMessage

        val uploadIds = plan.toUpload.mapNotNull { it.noteId?.toIntOrNull() }
        noteRepository.updateSyncStatus(uploadIds, NoteSyncStatus.SYNCING)

        // Phase 1 — uploads. Same Room id is always used as the Firestore
        // document id, so retries overwrite rather than duplicating.
        val uploadedLocalIds = mutableListOf<Int>()
        val failedUploadIds = mutableListOf<Int>()
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
                failedUploadIds += localId
                lastError = outcome.exceptionOrNull()?.message
                Log.w(TAG, "Failed to upload note id=$localId", outcome.exceptionOrNull())
            }
        }
        if (failedUploadIds.isNotEmpty()) {
            noteRepository.updateSyncStatus(failedUploadIds, NoteSyncStatus.FAILED)
        }

        // Phase 2 — local merge (upserts + sync flag flips) atomically.
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

        if (failed == 0) stampLastSyncTime(userId)

        return SyncResult(
            attempted = tombstoneResult.attempted + plan.toUpload.size + plan.toDownload.size,
            succeeded = succeeded,
            failed = failed,
            errorMessage = lastError.takeIf { failed > 0 }
        )
    }

    override suspend fun hasPendingNotes(): Boolean = noteRepository.hasPendingSyncNotes()

    /**
     * One-way push used when the remote fetch fails (rules, offline, etc.).
     * Still marks successfully uploaded Room rows as synced.
     */
    private suspend fun uploadLocalPendingOnly(userId: String): SyncResult {
        val pending = noteRepository.getPendingSyncNotes()
        if (pending.isEmpty()) {
            val tombstones = noteRepository.getTombstonedNotes()
            if (tombstones.isEmpty()) {
                return SyncResult(0, 0, 0, errorMessage = null)
            }
            return flushTombstones(userId, emptyList()).let {
                SyncResult(it.attempted, it.succeeded, it.failed, it.errorMessage)
            }
        }

        var succeeded = 0
        var failed = 0
        var lastError: String? = null
        val uploadedLocalIds = mutableListOf<Int>()
        val failedUploadIds = mutableListOf<Int>()
        val uploadIds = pending.mapNotNull { it.noteId?.toIntOrNull() }
        noteRepository.updateSyncStatus(uploadIds, NoteSyncStatus.SYNCING)

        for (note in pending) {
            val localId = note.noteId?.toIntOrNull()
            if (localId == null) {
                failed++
                continue
            }
            val outcome = runCatching { firestoreRepository.uploadNote(userId, note) }
            if (outcome.isSuccess) {
                uploadedLocalIds += localId
                succeeded++
                Log.d(TAG, "Uploaded pending note id=$localId for user=$userId")
            } else {
                failed++
                failedUploadIds += localId
                lastError = outcome.exceptionOrNull()?.message
                Log.w(TAG, "Failed to upload pending note id=$localId", outcome.exceptionOrNull())
            }
        }
        if (failedUploadIds.isNotEmpty()) {
            noteRepository.updateSyncStatus(failedUploadIds, NoteSyncStatus.FAILED)
        }
        if (uploadedLocalIds.isNotEmpty()) {
            runCatching {
                noteRepository.applyMergeResult(
                    remoteUpserts = emptyList(),
                    uploadedLocalIds = uploadedLocalIds
                )
            }.onFailure {
                failed += uploadedLocalIds.size
                succeeded = (succeeded - uploadedLocalIds.size).coerceAtLeast(0)
                lastError = it.message ?: lastError
                Log.w(TAG, "Failed to mark uploaded notes synced", it)
            }
        }
        if (failed == 0) stampLastSyncTime(userId)
        return SyncResult(
            attempted = pending.size,
            succeeded = succeeded,
            failed = failed,
            errorMessage = lastError.takeIf { failed > 0 }
        )
    }

    /**
     * Replicate local deletes to Firestore, then purge the Room tombstone.
     *
     * If the remote copy is newer than the tombstone, the remote version is
     * restored into Room instead of deleting — last-write-wins by `updatedAt`.
     * A failed remote delete leaves the tombstone in place for retry.
     */
    private suspend fun flushTombstones(
        userId: String,
        remoteNotes: List<Note>
    ): TombstoneFlushResult {
        val tombstones = noteRepository.getTombstonedNotes()
        if (tombstones.isEmpty()) return TombstoneFlushResult()

        val remoteById = remoteNotes
            .filter { !it.noteId.isNullOrBlank() }
            .associateBy { it.noteId!! }

        var succeeded = 0
        var failed = 0
        var lastError: String? = null
        val failedIds = mutableListOf<Int>()
        val restored = mutableListOf<Note>()
        val deletedRemoteIds = mutableListOf<String>()

        for (note in tombstones) {
            val localId = note.noteId?.toIntOrNull() ?: continue
            val remote = remoteById[note.noteId]
            if (remote != null &&
                remote.hasMeaningfulContent() &&
                remote.effectiveUpdatedMillis() > note.effectiveUpdatedMillis()
            ) {
                restored += remote
                succeeded++
                continue
            }
            val remoteDelete = runCatching {
                firestoreRepository.deleteNote(userId, localId.toString())
            }
            if (remoteDelete.isFailure) {
                failed++
                failedIds += localId
                lastError = remoteDelete.exceptionOrNull()?.message
                Log.w(TAG, "Failed to delete remote note id=$localId", remoteDelete.exceptionOrNull())
                continue
            }
            // Remote is gone (or never existed). Remember this id so the
            // subsequent merge cannot re-insert the stale fetched copy.
            deletedRemoteIds += localId.toString()
            val localDelete = runCatching {
                noteRepository.deleteNoteById(localId).collect { }
            }
            if (localDelete.isSuccess) {
                succeeded++
            } else {
                failed++
                failedIds += localId
                lastError = localDelete.exceptionOrNull()?.message ?: lastError
                Log.w(TAG, "Failed to purge local tombstone id=$localId", localDelete.exceptionOrNull())
            }
        }
        if (failedIds.isNotEmpty()) {
            noteRepository.updateSyncStatus(failedIds, NoteSyncStatus.FAILED)
        }
        if (restored.isNotEmpty()) {
            val restoreOutcome = runCatching {
                noteRepository.applyMergeResult(
                    remoteUpserts = restored,
                    uploadedLocalIds = emptyList()
                )
            }
            if (restoreOutcome.isFailure) {
                failed += restored.size
                succeeded -= restored.size
                lastError = restoreOutcome.exceptionOrNull()?.message ?: lastError
                Log.w(TAG, "Failed to restore newer remote notes over tombstones", restoreOutcome.exceptionOrNull())
                return TombstoneFlushResult(
                    attempted = tombstones.size,
                    succeeded = succeeded.coerceAtLeast(0),
                    failed = failed,
                    errorMessage = lastError.takeIf { failed > 0 },
                    deletedRemoteIds = deletedRemoteIds
                )
            }
        }
        return TombstoneFlushResult(
            attempted = tombstones.size,
            succeeded = succeeded.coerceAtLeast(0),
            failed = failed,
            errorMessage = lastError.takeIf { failed > 0 },
            restoredRemoteIds = restored.mapNotNull { it.noteId },
            deletedRemoteIds = deletedRemoteIds
        )
    }

    /**
     * Reconcile the two sides into deterministic upload/download buckets.
     *
     * Conflict keys use [com.app.domain.util.effectiveUpdatedMillis], which
     * prefers persisted millis columns and falls back to parsing legacy UI
     * timestamps. The larger value wins; equal timestamps are treated as
     * already converged and skipped.
     *
     * Valid local content is never replaced by an empty/null remote payload,
     * even when the remote timestamp is newer — the local copy is uploaded
     * instead so Firestore converges on the real data.
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
                localNote == null && remoteNote != null -> {
                    if (remoteNote.hasMeaningfulContent()) {
                        toDownload += remoteNote
                    }
                }
                localNote != null && remoteNote != null -> {
                    val localTs = localNote.effectiveUpdatedMillis()
                    val remoteTs = remoteNote.effectiveUpdatedMillis()
                    when {
                        localTs > remoteTs -> toUpload += localNote
                        remoteTs > localTs -> {
                            if (shouldProtectLocalFromEmptyRemote(localNote, remoteNote)) {
                                toUpload += localNote
                            } else {
                                toDownload += remoteNote
                            }
                        }
                        // Equal timestamps: still upload when Room marked the row pending.
                        NoteSyncStatus.needsUpload(localNote.isSync) -> toUpload += localNote
                    }
                }
            }
        }

        return MergePlan(toUpload = toUpload, toDownload = toDownload)
    }

    private fun shouldProtectLocalFromEmptyRemote(local: Note, remote: Note): Boolean {
        return local.hasMeaningfulContent() && !remote.hasMeaningfulContent()
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

    private data class TombstoneFlushResult(
        val attempted: Int = 0,
        val succeeded: Int = 0,
        val failed: Int = 0,
        val errorMessage: String? = null,
        val restoredRemoteIds: List<String> = emptyList(),
        /** Note ids successfully removed from Firestore (or confirmed absent). */
        val deletedRemoteIds: List<String> = emptyList()
    )

    private companion object {
        const val TAG = "SyncRepositoryImpl"
    }
}
