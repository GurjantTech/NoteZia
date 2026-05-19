package com.app.domain.usecase

import com.app.domain.model.UserProfile
import com.app.domain.repository.AuthRepository
import com.app.domain.repository.FirestoreRepository

/**
 * Drives the post-sign-in flow:
 *  1. Resolve the Google credential via [AuthRepository] (which also persists
 *     the session to DataStore).
 *  2. Persist the profile to Firestore (`users/{userId}`).
 *
 * The actual upload of locally pending notes (`isSync = 0`) is orchestrated
 * by [com.app.domain.usecase.SyncPendingNotesUseCase] from the
 * `CloudSyncViewModel` immediately after this use case returns — keeping
 * sign-in and sync as separate, individually retriable steps while still
 * presenting a single "logged-in then synced" experience to the user.
 *
 * Returns the resolved [UserProfile]. Throws on cancel/failure.
 */
class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) {
    suspend operator fun invoke(): UserProfile {
        val profile = authRepository.signInWithGoogle()
        // Best-effort: profile upload failure shouldn't block the user from
        // signing in — the next sync pass will re-attempt as part of the
        // user-document write triggered by `lastSyncTime`.
        runCatching { firestoreRepository.saveUserProfile(profile) }
        return profile
    }
}
