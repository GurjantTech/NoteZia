package com.app.data.repository

import com.app.data.auth.GoogleAuthManager
import com.app.data.auth.UserSessionStorage
import com.app.domain.model.UserProfile
import com.app.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Default [AuthRepository] backed by Credential Manager (for the sign-in
 * UX) and DataStore (for session persistence). FirebaseAuth is intentionally
 * absent — the spec requires the standalone Google Sign-In flow only.
 *
 * [signOutClient] is provided by Hilt; we only need it for the explicit
 * `GoogleSignInClient.signOut()` call required by the spec to revoke the
 * cached account selection in the system UI.
 */
class AuthRepositoryImpl(
    private val googleAuthManager: GoogleAuthManager,
    private val sessionStorage: UserSessionStorage,
    private val signOutClient: GoogleSignInClient
) : AuthRepository {

    override suspend fun signInWithGoogle(): UserProfile {
        val profile = googleAuthManager.signIn()
        sessionStorage.saveUser(profile)
        return profile
    }

    override fun observeCurrentUser(): Flow<UserProfile?> = sessionStorage.currentUser

    override suspend fun getCurrentUser(): UserProfile? = sessionStorage.snapshot()

    override suspend fun signOut() {
        runCatching { signOutClient.signOut().await() }
        sessionStorage.clear()
    }
}
