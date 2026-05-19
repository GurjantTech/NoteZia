package com.appgurjant.stickynotes.ui.screens

import androidx.lifecycle.ViewModel
import com.app.domain.usecase.IsOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val isOnboardingCompletedUseCase: IsOnboardingCompletedUseCase
) : ViewModel() {

    suspend fun isOnboardingCompleted(): Boolean = isOnboardingCompletedUseCase()
}
