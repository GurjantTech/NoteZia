package com.app.domain.usecase

import com.app.domain.model.Note
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository

/**
 * Uploads a single note to Firestore for the signed-in user. Used by the
 * SyncWorker per-note loop and by the optional "publish immediately" path
 * after a save.
 */
class UploadNoteUseCase(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(note: Note): Boolean {
        val user = authRepository.getCurrentUser() ?: return false
        return runCatching {
            firestoreRepository.uploadNote(user.userId, note)
        }.isSuccess
    }
}
