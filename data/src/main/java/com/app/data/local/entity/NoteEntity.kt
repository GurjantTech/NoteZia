package com.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room representation of a note.
 *
 * [isSync]: 0 = pending upload, 1 = synced. Mirrors `Note.isSync` in the
 * domain model. Migrated from schema v2 → v3 (see `AppModule`'s
 * `Migration_2_3`); existing rows default to 0 so they are uploaded on the
 * first sync pass after the user signs in.
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
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val reminderAtMillis: Long? = null,
    val textStyleJson: String? = null
)
