package com.app.domain.usecase

import com.app.domain.model.UserProfile
import com.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/** Reactive accessor for the currently signed-in user (or null). */
class ObserveCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserProfile?> = authRepository.observeCurrentUser()
}
