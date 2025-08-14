package com.app.domain.usecase

import com.app.domain.model.Note
import com.app.domain.repository.NoteRepository
import com.app.domain.utils.UIState
import kotlinx.coroutines.flow.Flow

class AllNoteUseCase (private val repository: NoteRepository) {
    suspend operator fun invoke() : Flow<List<Note>> {
        return repository.getAllNotes()
    }
}