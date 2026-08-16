package com.app.domain.usecase

import com.app.domain.repository.AuthRepository
import com.app.domain.repository.SyncScheduler

/**
 * Runs an in-process Firestore sync when signed in, then also schedules
 * WorkManager as a backup for offline / retry cases.
 *
 * Logged-out saves stay local in Room with no network work.
 */
class RequestSyncUseCase(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    private val syncPendingNotesUseCase: SyncPendingNotesUseCase
) {
    suspend operator fun invoke() {
        if (authRepository.getCurrentUser() == null) return
        // Prefer an immediate in-app sync so notes appear in Firestore without
        // waiting on WorkManager quotas / delays.
        runCatching { syncPendingNotesUseCase() }
        syncScheduler.ensurePeriodicSync()
        syncScheduler.requestImmediateSync()
    }
}
