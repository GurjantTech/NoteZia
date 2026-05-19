package com.app.domain.usecase

import com.app.domain.model.StandardResponse
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository
import com.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deletes a note while keeping local + cloud state consistent.
 *
 * Deletion is intentionally the **only** flow that still touches Firestore
 * directly (note creation and edits are now strictly local; cloud uploads
 * happen via the manual "Sync My Notes" action). Deleting a note is a
 * destructive user action and must be replicated immediately so it cannot
 * "come back" on the next manual sync.
 *
 * Flow:
 *
 * 1. **Signed in**: delete the Firestore document at
 *    `users/{userId}/notes/{noteId}` first. Only when that succeeds is the
 *    local row purged. On failure the row is preserved locally and an
 *    error response is emitted so the UI can prompt the user.
 *
 * 2. **Signed out**: a local-only delete is performed — there is no remote
 *    document to reconcile against.
 *
 * The single returned [StandardResponse] flow exposes either
 * `status = "success"` or `status = "error"` so the call site can route to
 * the right toast / navigation.
 */
class DeleteNoteUseCase(
    private val noteRepository: NoteRepository,
    private val firestoreRepository: FirestoreRepository,
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(noteId: Int): Flow<StandardResponse> = flow {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            val remote = runCatching {
                firestoreRepository.deleteNote(user.userId, noteId.toString())
            }
            if (remote.isFailure) {
                emit(
                    StandardResponse(
                        status = STATUS_ERROR,
                        message = remote.exceptionOrNull()?.message
                            ?: "Failed to delete note from cloud. Please try again."
                    )
                )
                return@flow
            }
        }

        // Local hard-delete (always runs once any remote step has succeeded
        // or when the user is signed out).
        noteRepository.deleteNoteById(noteId).collect { response ->
            emit(response)
        }
    }

    companion object {
        const val STATUS_ERROR = "error"
        const val STATUS_SUCCESS = "success"
    }
}
