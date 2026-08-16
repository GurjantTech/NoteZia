package com.app.domain.usecase

import com.app.domain.model.PhoneVerificationResult
import com.app.domain.model.UserProfile
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository

class SendPhoneVerificationCodeUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        phoneNumber: String,
        activityHost: Any
    ): PhoneVerificationResult = authRepository.sendPhoneVerificationCode(phoneNumber, activityHost)
}

class VerifyPhoneCodeUseCase(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val activateCloudSyncUseCase: ActivateCloudSyncUseCase
) {
    suspend operator fun invoke(verificationId: String, code: String): UserProfile {
        val profile = authRepository.verifyPhoneCode(verificationId, code)
        runCatching { firestoreRepository.saveUserProfile(profile) }
        activateCloudSyncUseCase(profile.userId)
        return profile
    }
}

class CompletePhoneAutoSignInUseCase(
    private val firestoreRepository: FirestoreRepository,
    private val activateCloudSyncUseCase: ActivateCloudSyncUseCase
) {
    suspend operator fun invoke(profile: UserProfile): UserProfile {
        runCatching { firestoreRepository.saveUserProfile(profile) }
        activateCloudSyncUseCase(profile.userId)
        return profile
    }
}
