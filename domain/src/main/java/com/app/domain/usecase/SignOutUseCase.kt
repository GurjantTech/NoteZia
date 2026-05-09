package com.app.domain.usecase

import com.app.domain.repository.AuthRepository
import com.app.domain.repository.SyncScheduler

/**
 * Signs the user out of Google, clears local session state, and cancels any
 * pending sync work. Local notes remain untouched per spec.
 */
class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler
) {
    suspend operator fun invoke() {
        syncScheduler.cancelAll()
        authRepository.signOut()
    }
}
