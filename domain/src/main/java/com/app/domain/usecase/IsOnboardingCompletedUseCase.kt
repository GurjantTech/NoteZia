package com.app.domain.usecase

import com.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first

class IsOnboardingCompletedUseCase(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke(): Boolean =
        onboardingRepository.isOnboardingCompleted().first()
}
