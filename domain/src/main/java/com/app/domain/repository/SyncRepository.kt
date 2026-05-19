package com.app.domain.repository

import com.app.domain.model.SyncResult

/**
 * Orchestrates the actual upload of pending local notes to Firestore.
 *
 * Implementations are responsible for:
 * - Reading every local note where `isSync = 0`.
 * - Uploading each via [FirestoreRepository.uploadNote].
 * - Marking successfully uploaded rows with `isSync = 1`.
 * - Refreshing `lastSyncTime` in Firestore + DataStore on success.
 *
 * All work is performed on the suspending caller's context. Returns a
 * [SyncResult] summary; never throws — failures are folded into the result so
 * Workers/UI can decide whether to retry.
 */
interface SyncRepository {

    /** Push every pending local note for [userId]. Returns a summary. */
    suspend fun syncPendingNotes(userId: String): SyncResult

    /** Convenience: returns true if at least one note has `isSync = 0`. */
    suspend fun hasPendingNotes(): Boolean
}
