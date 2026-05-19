package com.app.domain.repository

import com.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithGoogle(): UserProfile

    /** Cold flow that emits the currently signed-in user, or null when logged out. */
    fun observeCurrentUser(): Flow<UserProfile?>

    /** Snapshot accessor used by sync workers / repositories that can't collect a flow. */
    suspend fun getCurrentUser(): UserProfile?
    suspend fun signOut()
}
