package com.appgurjant.stickynotes.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.domain.usecase.SetOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase
) : ViewModel() {

    suspend fun completeOnboarding() {
        setOnboardingCompletedUseCase()
    }

    fun completeOnboardingAsync(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            completeOnboarding()
            onDone()
        }
    }
}
