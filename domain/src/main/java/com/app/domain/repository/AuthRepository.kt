package com.app.domain.repository

import com.app.domain.model.PhoneVerificationResult
import com.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithGoogle(): UserProfile

    /**
     * Starts Firebase Phone Authentication for [phoneNumber].
     *
     * [activityHost] must be an Android [android.app.Activity] instance — the
     * data layer casts it for the Firebase reCAPTCHA / SMS flow.
     */
    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        activityHost: Any
    ): PhoneVerificationResult

    suspend fun verifyPhoneCode(verificationId: String, code: String): UserProfile

    /** Cold flow that emits the currently signed-in Firebase user, or null when logged out. */
    fun observeCurrentUser(): Flow<UserProfile?>

    /** Snapshot accessor used by sync workers / repositories that can't collect a flow. */
    suspend fun getCurrentUser(): UserProfile?

    /**
     * Last Firebase uid that signed in on this device. Survives logout so
     * that user's local notes stay visible while logged out.
     */
    fun observeLastSignedInUserId(): Flow<String?>

    fun observeSyncBannerDismissed(): Flow<Boolean>

    suspend fun dismissSyncBanner()

    suspend fun signOut()
}
