package com.app.domain.usecase

import com.app.domain.repository.OnboardingRepository

class SetOnboardingCompletedUseCase(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke() {
        onboardingRepository.setOnboardingCompleted(completed = true)
    }
}
