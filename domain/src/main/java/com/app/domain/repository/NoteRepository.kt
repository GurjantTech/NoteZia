package com.app.domain.repository

import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.utils.UIState
import kotlinx.coroutines.flow.Flow


interface NoteRepository {
    suspend fun insertNote(note: Note): Flow<UIState>
    suspend fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteDetailFromLocalById(notedId: String): Flow<Note>
    suspend fun updateNoteDetailInLocal(noted: Note): Flow<StandardResponse>
    suspend fun deleteNoteById(noteId: Int): Flow<StandardResponse>
}