package com.app.domain.usecase

import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class UpdateNoteDetailFromLocalUseCase (private val repository: NoteRepository) {
    suspend operator fun invoke(note: Note) : Flow<StandardResponse> {
        return repository.updateNoteDetailInLocal(note)
    }
}