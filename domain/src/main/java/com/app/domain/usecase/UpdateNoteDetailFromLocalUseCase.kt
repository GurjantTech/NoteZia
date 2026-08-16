package com.app.domain.usecase

import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Updates a note in Room, then schedules a background Firebase sync for the
 * same note id. The mapper resets the row to pending so retries stay
 * idempotent and never create a duplicate document.
 */
class UpdateNoteDetailFromLocalUseCase(
    private val repository: NoteRepository,
    private val authRepository: AuthRepository,
    private val requestSyncUseCase: RequestSyncUseCase
) {
    suspend operator fun invoke(note: Note): Flow<StandardResponse> {
        val stamped = stampOwner(note)
        return repository.updateNoteDetailInLocal(stamped).onEach { response ->
            if (response.status.equals("success", ignoreCase = true)) {
                requestSyncUseCase()
            }
        }
    }

    private suspend fun stampOwner(note: Note): Note {
        val owner = authRepository.getCurrentUser()?.userId ?: return note
        return note.copy(ownerUserId = owner)
    }
}
