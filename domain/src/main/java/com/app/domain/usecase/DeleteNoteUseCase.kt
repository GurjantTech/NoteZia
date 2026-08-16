package com.app.domain.usecase

import com.app.domain.model.StandardResponse
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deletes a note while keeping local + cloud state consistent.
 *
 * Signed-in users get an immediate local tombstone so the note disappears
 * from the UI even when offline. The background sync worker then deletes
 * the matching Firestore document (same note id) and purges the Room row.
 * A remote failure leaves the tombstone in place for retry — the local
 * note is never resurrected just because Firebase is unavailable.
 *
 * Signed-out users get a local-only hard delete; there is no remote
 * document to reconcile against.
 */
class DeleteNoteUseCase(
    private val noteRepository: NoteRepository,
    private val authRepository: AuthRepository,
    private val requestSyncUseCase: RequestSyncUseCase
) {

    suspend operator fun invoke(noteId: Int): Flow<StandardResponse> = flow {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            val hidden = noteRepository.tombstoneNote(noteId)
            if (!hidden) {
                emit(
                    StandardResponse(
                        status = STATUS_ERROR,
                        message = "Failed to delete note. Please try again."
                    )
                )
                return@flow
            }
            requestSyncUseCase()
            emit(
                StandardResponse(
                    status = STATUS_SUCCESS,
                    message = "Note deleted successfully"
                )
            )
            return@flow
        }

        noteRepository.deleteNoteById(noteId).collect { response ->
            emit(response)
        }
    }

    companion object {
        const val STATUS_ERROR = "error"
        const val STATUS_SUCCESS = "success"
    }
}
