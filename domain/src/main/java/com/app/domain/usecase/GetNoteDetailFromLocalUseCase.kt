package com.app.domain.usecase

import com.app.domain.model.Note
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetNoteDetailFromLocalUseCase (private val repository: NoteRepository) {
    suspend operator fun invoke(notedId: String) : Flow<Note> {
        return repository.getNoteDetailFromLocalById(notedId)
    }
}