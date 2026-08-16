package com.app.domain.usecase

import com.app.domain.model.UserProfile
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository

/**
 * Drives the post Google sign-in flow:
 *  1. Google credential → Firebase Authentication.
 *  2. Persist the profile to Firestore at `users/{firebaseUid}`.
 *  3. Claim pending local notes and start automatic cloud sync.
 */
class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val activateCloudSyncUseCase: ActivateCloudSyncUseCase
) {
    suspend operator fun invoke(): UserProfile {
        val profile = authRepository.signInWithGoogle()
        runCatching { firestoreRepository.saveUserProfile(profile) }
        activateCloudSyncUseCase(profile.userId)
        return profile
    }
}
