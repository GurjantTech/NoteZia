package com.app.domain.model

/**
 * Domain representation of a single note.
 *
 * [isSync] mirrors the local Room column via [NoteSyncStatus]:
 * `0` pending, `1` synced, `2` syncing, `3` failed.
 * Default `0` keeps every newly-created or edited note queued for automatic
 * background upload after the Room write succeeds.
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
    /** Firebase uid that owns this note locally; null = unclaimed / pre-login. */
    var ownerUserId: String? = null,
    /** Epoch millis when the note was first created (stable across edits). */
    var createdAtMillis: Long = 0L,
    /** Epoch millis of last modification — drives conflict resolution with Firestore [updatedAt]. */
    var updatedAtMillis: Long = 0L,
    /** Optional reminder instant; null when unset. */
    var reminderAtMillis: Long? = null
)