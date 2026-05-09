package com.app.domain.model

/**
 * Domain representation of a single note.
 *
 * [isSync] mirrors the local Room column: `0` = pending upload, `1` = synced.
 * Default `0` keeps every newly-created note flagged for the next manual
 * "Sync My Notes" pass.
 */
data class Note(
    var noteId: String? = "",
    var title: String? = "",
    var description: String? = null,
    var timeStamp: String? = "",
    var contentJson: String? = "",
    var noteType: String? = "",
    var textStyleConfig: TextStyleConfig? = TextStyleConfig(),
    var isSync: Int = 0,
    /** Epoch millis when the note was first created (stable across edits). */
    var createdAtMillis: Long = 0L,
    /** Epoch millis of last modification — drives conflict resolution with Firestore [updatedAt]. */
    var updatedAtMillis: Long = 0L,
    /** Optional reminder instant; null when unset. */
    var reminderAtMillis: Long? = null
)