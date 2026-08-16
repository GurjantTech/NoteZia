package com.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room representation of a note.
 *
 * [isSync]: persisted [com.app.domain.model.NoteSyncStatus] —
 * 0 pending, 1 synced, 2 syncing, 3 failed. Migrated from schema v2 → v3
 * (see `AppModule`'s `Migration_2_3`); existing rows default to 0 so they
 * upload automatically after the user signs in.
 *
 * [isDeleted]: local tombstone for offline-safe deletion. Signed-in deletes
 * flag the row here first; the sync worker then removes the Firestore
 * document and hard-deletes the local row. Active queries exclude `1`.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String? = "",
    val description: String? = "",
    val timeStamp: String? = "",
    val contentJson: String? = "",
    val noteType: String? = "",
    val isSync: Int = 0,
    val isDeleted: Int = 0,
    /** Firebase uid that owns this note; null = created while logged out. */
    val ownerUserId: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val reminderAtMillis: Long? = null,
    val textStyleJson: String? = null
)
