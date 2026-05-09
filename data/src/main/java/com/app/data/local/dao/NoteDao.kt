package com.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.app.data.local.entity.NoteEntity


@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timeStamp DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(noteEntity: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Int): Int

    @Query("SELECT * FROM notes WHERE id =:noteId LIMIT 1")
    suspend fun getNoteById(noteId: Int): NoteEntity?

    @Update
    suspend fun updateNote(noteEntity: NoteEntity): Int

    /** Notes that haven't been pushed to Firestore yet. */
    @Query("SELECT * FROM notes WHERE isSync = 0")
    suspend fun getPendingSyncNotes(): List<NoteEntity>

    /** Set isSync = 1 for a single note (used after a successful upload). */
    @Query("UPDATE notes SET isSync = 1 WHERE id = :noteId")
    suspend fun markNoteSynced(noteId: Int): Int

    /** Reset all rows to pending — used after sign-in to force a full re-push. */
    @Query("UPDATE notes SET isSync = 0")
    suspend fun markAllPending(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isSync = 0")
    suspend fun countPendingSyncNotes(): Int

    /**
     * Atomic local side of the two-way merge.
     *
     * Each entry of [remoteUpserts] carries an explicit primary key — Room's
     * [OnConflictStrategy.REPLACE] therefore overwrites the matching local
     * row when it already exists or inserts a new one with the exact same id
     * (keeping the local row's id in sync with its Firestore document id).
     *
     * [uploadedLocalIds] flips successfully-uploaded rows to `isSync = 1`
     * inside the same transaction so a crash partway through cannot leave
     * stale "pending" flags or partially-applied remote rows.
     */
    @Transaction
    suspend fun applyMergeResult(
        remoteUpserts: List<NoteEntity>,
        uploadedLocalIds: List<Int>
    ) {
        for (entity in remoteUpserts) saveNote(entity)
        for (id in uploadedLocalIds) markNoteSynced(id)
    }
}
