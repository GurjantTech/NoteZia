package com.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.data.entites.StandardResponseEntity
import com.app.data.local.entity.NoteEntity
import com.app.domain.model.Note
import com.app.domain.model.StandardResponse

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
    suspend fun updateNote(noteEntity: NoteEntity):Int
}