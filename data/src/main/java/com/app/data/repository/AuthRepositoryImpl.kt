package com.app.data.repository

import android.app.Activity
import com.app.data.auth.FirebaseAuthManager
import com.app.data.auth.UserSessionStorage
import com.app.domain.model.PhoneVerificationResult
import com.app.domain.model.UserProfile
import com.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Firebase Authentication-backed [AuthRepository].
 *
 * Session state is driven by [FirebaseAuthManager.observeAuthState]; DataStore
 * caches profile metadata (e.g. [UserProfile.lastSyncTime]) for offline reads.
 */
class AuthRepositoryImpl(
    private val firebaseAuthManager: FirebaseAuthManager,
    private val sessionStorage: UserSessionStorage
) : AuthRepository {

    override suspend fun signInWithGoogle(): UserProfile {
        val profile = firebaseAuthManager.signInWithGoogle()
        sessionStorage.saveUser(profile)
        return profile
    }

    override suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        activityHost: Any
    ): PhoneVerificationResult {
        val activity = activityHost as? Activity
            ?: error("Phone authentication requires an Activity context")
        val result = firebaseAuthManager.sendPhoneVerificationCode(phoneNumber, activity)
        if (result is PhoneVerificationResult.AutoVerified) {
            sessionStorage.saveUser(result.profile)
        }
        return result
    }

    override suspend fun verifyPhoneCode(verificationId: String, code: String): UserProfile {
        val profile = firebaseAuthManager.verifyPhoneCode(verificationId, code)
        sessionStorage.saveUser(profile)
        return profile
    }

    override fun observeCurrentUser(): Flow<UserProfile?> =
        firebaseAuthManager.observeAuthState().map { firebaseUser ->
            firebaseUser?.let { mergeCachedMetadata(it) }
        }

    override suspend fun getCurrentUser(): UserProfile? =
        firebaseAuthManager.currentUser()?.let { mergeCachedMetadata(it) }

    override fun observeLastSignedInUserId(): Flow<String?> = sessionStorage.lastSignedInUserId

    override fun observeSyncBannerDismissed(): Flow<Boolean> = sessionStorage.syncBannerDismissed

    override suspend fun dismissSyncBanner() {
        sessionStorage.dismissSyncBanner()
    }

    override suspend fun signOut() {
        firebaseAuthManager.signOut()
        sessionStorage.clear()
    }

    private suspend fun mergeCachedMetadata(profile: UserProfile): UserProfile {
        val cached = sessionStorage.snapshot()
        return if (cached?.userId == profile.userId && cached.lastSyncTime > 0L) {
            profile.copy(lastSyncTime = cached.lastSyncTime)
        } else {
            profile
        }
    }
}
