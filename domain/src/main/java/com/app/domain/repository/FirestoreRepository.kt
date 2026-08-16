package com.app.domain.repository

import com.app.domain.model.Note
import com.app.domain.model.UserProfile

/**
 * Cloud-side persistence contract.
 *
 * Implementations write to `users/{userId}` and `users/{userId}/notes/{noteId}`.
 * All methods are suspend and throw on network/IO errors so the calling
 * use case can surface user-friendly messages or schedule retries.
 */
interface FirestoreRepository {

    /** Creates or merges the user profile document at `users/{userId}`. */
    suspend fun saveUserProfile(profile: UserProfile)

    /** Uploads a single note as `users/{userId}/notes/{noteId}`. */
    suspend fun uploadNote(userId: String, note: Note)

    /**
     * Returns every non-deleted note stored under `users/{userId}/notes`.
     * Used by the two-way merge to discover remote-only or remote-newer
     * documents during an automatic background sync pass.
     */
    suspend fun fetchAllNotes(userId: String): List<Note>

    /**
     * Permanently removes the document at `users/{userId}/notes/{noteId}`.
     * Called by the sync worker once a local tombstone has been detected;
     * the local row is hard-deleted only after this call succeeds, so a
     * failed delete is automatically retried on the next sync pass.
     */
    suspend fun deleteNote(userId: String, noteId: String)

    /** Updates only the `lastSyncTime` field on the user document. */
    suspend fun updateLastSyncTime(userId: String, timestamp: Long)
}
