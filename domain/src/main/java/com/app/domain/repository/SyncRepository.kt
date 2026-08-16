package com.app.domain.repository

import com.app.domain.model.SyncResult

/**
 * Orchestrates two-way synchronization between Room and Firestore.
 *
 * Implementations are responsible for:
 * - Reading every local note that is not yet synced (and any tombstones).
 * - Uploading each via [FirestoreRepository.uploadNote] using the existing
 *   Room id as the Firestore document id (idempotent, no duplicates).
 * - Downloading remote-newer notes without overwriting valid local data
 *   with empty/null remote payloads.
 * - Marking successfully uploaded rows as synced; failed rows stay pending.
 * - Refreshing `lastSyncTime` in Firestore + DataStore on a clean pass.
 *
 * All work is performed on the suspending caller's context. Returns a
 * [SyncResult] summary; never throws — failures are folded into the result so
 * Workers can decide whether to retry. Local Room data is never deleted
 * because Firestore is unavailable.
 */
interface SyncRepository {

    /** Push every pending local note for [userId]. Returns a summary. */
    suspend fun syncPendingNotes(userId: String): SyncResult

    /** Convenience: returns true if at least one note has `isSync = 0`. */
    suspend fun hasPendingNotes(): Boolean
}
