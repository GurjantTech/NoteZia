package com.app.domain.usecase

import com.app.domain.repository.AuthRepository
import com.app.domain.repository.SyncScheduler

/**
 * Generic "schedule a sync now" trigger. Callers don't need to know whether
 * the user is signed in — the use case checks for them.
 */
class RequestSyncUseCase(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) {
    suspend operator fun invoke() {
        if (authRepository.getCurrentUser() != null) {
            syncScheduler.requestImmediateSync()
        }
    }
}
