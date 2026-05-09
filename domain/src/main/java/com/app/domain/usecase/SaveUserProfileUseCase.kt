package com.app.domain.usecase

import com.app.domain.model.UserProfile
import com.app.domain.repository.FirestoreRepository

/** Persists the user profile to Firestore (idempotent — safe to retry). */
class SaveUserProfileUseCase(
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(profile: UserProfile) {
        firestoreRepository.saveUserProfile(profile)
    }
}
