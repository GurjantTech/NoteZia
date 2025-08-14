package com.app.domain.usecase

import com.app.domain.model.StandardResponse
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class DeleteNoteUseCase (private val repository: NoteRepository) {

    suspend operator fun invoke(noteId: Int): Flow<StandardResponse> {
        return repository.deleteNoteById(noteId)
    }
}