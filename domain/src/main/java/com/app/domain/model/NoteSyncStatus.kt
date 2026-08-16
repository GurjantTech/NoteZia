package com.app.domain.model

/**
 * Per-note cloud-sync state persisted in Room's `isSync` column.
 *
 * Kept as integer constants so existing databases continue to work without
 * a schema migration: `0` and `1` already meant pending / synced.
 */
object NoteSyncStatus {
    const val PENDING = 0
    const val SYNCED = 1
    const val SYNCING = 2
    const val FAILED = 3

    fun isSynced(code: Int): Boolean = code == SYNCED

    /** Anything other than [SYNCED] still needs a background upload pass. */
    fun needsUpload(code: Int): Boolean = code != SYNCED
}

/** True when the note carries any user-visible payload worth preserving. */
fun Note.hasMeaningfulContent(): Boolean {
    return !title.isNullOrBlank() ||
        !description.isNullOrBlank() ||
        !contentJson.isNullOrBlank()
}
