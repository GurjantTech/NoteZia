package com.app.domain.usecase

import com.app.domain.model.SyncResult
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.SyncRepository

/**
 * Runs a two-way Room ↔ Firestore sync for the currently signed-in user.
 * If no user is signed in this is a no-op and returns an empty result —
 * matching spec: "If user is not logged in: keep notes local only".
 */
class SyncPendingNotesUseCase(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): SyncResult {
        val user = authRepository.getCurrentUser()
            ?: return SyncResult(0, 0, 0, errorMessage = null)
        return syncRepository.syncPendingNotes(user.userId)
    }
}
