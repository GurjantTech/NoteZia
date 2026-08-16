package com.app.domain.repository

import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import kotlinx.coroutines.flow.Flow


interface NoteRepository {
    suspend fun insertNote(note: Note): Flow<String>
    suspend fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteDetailFromLocalById(notedId: String): Flow<Note>
    suspend fun updateNoteDetailInLocal(noted: Note): Flow<StandardResponse>

    /**
     * Permanently removes the row from Room. Cloud replication is now the
     * use case's responsibility (see `DeleteNoteUseCase`) — the repository
     * deals with local persistence only.
     */
    suspend fun deleteNoteById(noteId: Int): Flow<StandardResponse>

    /** Returns every active local note that is not yet [com.app.domain.model.NoteSyncStatus.SYNCED]. */
    suspend fun getPendingSyncNotes(): List<Note>

    /** Mark a single local note as synced (`isSync = 1`). */
    suspend fun markNoteSynced(noteId: Int)

    /** Flip [noteIds] to the given [com.app.domain.model.NoteSyncStatus] value. */
    suspend fun updateSyncStatus(noteIds: List<Int>, status: Int)

    /**
     * Hide [noteId] locally (tombstone) so the UI drops it immediately while
     * the background worker replicates the delete to Firestore.
     */
    suspend fun tombstoneNote(noteId: Int): Boolean

    /** Soft-deleted local rows waiting for a remote delete + local purge. */
    suspend fun getTombstonedNotes(): List<Note>

    /** True if at least one local note is awaiting upload or a tombstone flush. */
    suspend fun hasPendingSyncNotes(): Boolean

    /**
     * Snapshot of every *active* local note (no Flow wrapper, tombstones
     * excluded) — used by the two-way sync merge to compare against remote.
     */
    suspend fun getAllNotesSnapshot(): List<Note>

    /**
     * Apply the local side of a two-way merge in a single Room transaction:
     *  - [remoteUpserts]: notes downloaded from Firestore that should be
     *    inserted/replaced locally (each carries an explicit `noteId` so the
     *    Room primary key stays in lockstep with the Firestore document id).
     *  - [uploadedLocalIds]: ids of local rows we just successfully pushed to
     *    Firestore — flipped to `isSync = 1` inside the same transaction so a
     *    crash mid-merge cannot leave the queue inconsistent.
     */
    suspend fun applyMergeResult(
        remoteUpserts: List<Note>,
        uploadedLocalIds: List<Int>
    )

    /**
     * Stamp every active note without an [Note.ownerUserId] with [userId] so
     * they are never uploaded to a different Firebase account.
     */
    suspend fun claimUnownedNotesForUser(userId: String)

    /**
     * Mark every active note owned by [userId] as pending so the next sync
     * pass uploads them to Firestore (including notes created before sign-in).
     */
    suspend fun markOwnedNotesPending(userId: String)
}