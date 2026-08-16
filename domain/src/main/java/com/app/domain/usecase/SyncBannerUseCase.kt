package com.app.domain.usecase

import com.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveSyncBannerDismissedUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> = authRepository.observeSyncBannerDismissed()
}

class DismissSyncBannerUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.dismissSyncBanner()
}
