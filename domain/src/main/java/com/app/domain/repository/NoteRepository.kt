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

    /** Returns every local note with `isSync = 0`. */
    suspend fun getPendingSyncNotes(): List<Note>

    /** Mark a single local note as synced (`isSync = 1`). */
    suspend fun markNoteSynced(noteId: Int)

    /** True if at least one local note is awaiting upload. */
    suspend fun hasPendingSyncNotes(): Boolean

    /**
     * Snapshot of every local note (no Flow wrapper) — used by the two-way
     * sync merge to compare against the remote state.
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
}