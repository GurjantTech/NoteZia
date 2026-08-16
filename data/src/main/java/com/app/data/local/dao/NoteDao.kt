package com.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY timeStamp DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY timeStamp DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(noteEntity: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Int): Int

    @Query("SELECT * FROM notes WHERE id =:noteId LIMIT 1")
    suspend fun getNoteById(noteId: Int): NoteEntity?

    @Update
    suspend fun updateNote(noteEntity: NoteEntity): Int

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isSync != 1")
    suspend fun getPendingSyncNotes(): List<NoteEntity>

    @Query("UPDATE notes SET isSync = 1 WHERE id = :noteId")
    suspend fun markNoteSynced(noteId: Int): Int

    @Query("UPDATE notes SET isSync = :status WHERE id IN (:noteIds)")
    suspend fun updateSyncStatus(noteIds: List<Int>, status: Int): Int

    @Query("UPDATE notes SET isSync = 0")
    suspend fun markAllPending(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 0 AND isSync != 1")
    suspend fun countPendingSyncNotes(): Int

    @Query(
        """
        UPDATE notes
        SET isDeleted = 1,
            isSync = 0,
            updatedAtMillis = :now,
            timeStamp = :nowText
        WHERE id = :noteId
        """
    )
    suspend fun tombstoneNote(noteId: Int, now: Long, nowText: String): Int

    @Query("SELECT * FROM notes WHERE isDeleted = 1")
    suspend fun getTombstonedNotes(): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    suspend fun countTombstonedNotes(): Int

    /** Assign unclaimed local notes to [userId] and mark them pending for upload. */
    @Query(
        """
        UPDATE notes
        SET ownerUserId = :userId,
            isSync = 0
        WHERE isDeleted = 0
          AND (ownerUserId IS NULL OR ownerUserId = '')
        """
    )
    suspend fun claimUnownedNotesForUser(userId: String): Int

    /** Queue every active note owned by [userId] for a Firestore upload pass. */
    @Query(
        """
        UPDATE notes
        SET isSync = 0
        WHERE isDeleted = 0
          AND ownerUserId = :userId
        """
    )
    suspend fun markOwnedNotesPending(userId: String): Int

    @Transaction
    suspend fun applyMergeResult(
        remoteUpserts: List<NoteEntity>,
        uploadedLocalIds: List<Int>
    ) {
        for (entity in remoteUpserts) saveNote(entity)
        for (id in uploadedLocalIds) markNoteSynced(id)
    }
}
