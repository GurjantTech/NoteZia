package com.app.domain.usecase

import com.app.domain.model.UserProfile
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository
import com.app.domain.repository.SyncScheduler

/**
 * Drives the post-sign-in flow:
 *  1. Resolve the Google credential via [AuthRepository].
 *  2. Persist the profile to Firestore (`users/{userId}`).
 *  3. Kick off an immediate sync so any locally created notes are uploaded.
 *
 * Returns the resolved [UserProfile]. Throws on cancel/failure.
 */
class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val syncScheduler: SyncScheduler
) {
    suspend operator fun invoke(): UserProfile {
        val profile = authRepository.signInWithGoogle()
        // Best-effort: failure here shouldn't block the user from signing in;
        // the next sync pass will re-attempt the profile upload.
        runCatching { firestoreRepository.saveUserProfile(profile) }
        syncScheduler.requestImmediateSync()
        return profile
    }
}
