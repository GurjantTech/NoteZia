package com.app.domain.usecase

import com.app.domain.model.Note
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Inserts a note into Room. Background Firebase sync is requested only when
 * a Firebase user is authenticated; otherwise the note stays local.
 */
class AddNoteUseCase(
    private val repository: NoteRepository,
    private val authRepository: AuthRepository,
    private val requestSyncUseCase: RequestSyncUseCase
) {

    suspend operator fun invoke(note: Note): Flow<String> {
        val stamped = stampOwner(note)
        return flow {
            repository.insertNote(stamped).collect { message ->
                emit(message)
                if (message.contains("successfully", ignoreCase = true)) {
                    // Always request sync after a successful local write while
                    // signed in. WorkManager uploads to Firestore in the background.
                    requestSyncUseCase()
                }
            }
        }
    }

    private suspend fun stampOwner(note: Note): Note {
        val owner = authRepository.getCurrentUser()?.userId ?: return note
        return note.copy(ownerUserId = owner)
    }
}
