package com.app.domain.usecase

import com.app.domain.model.Note
import com.app.domain.repository.NoteRepository
import com.app.domain.utils.UIState
import kotlinx.coroutines.flow.Flow


class AddNoteUseCase(private val repository: NoteRepository) {

    suspend operator fun invoke(note: Note) : Flow<UIState> {
        return repository.insertNote(note)
    }
}