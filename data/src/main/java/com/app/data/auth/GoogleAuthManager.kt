package com.app.data.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.app.domain.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONObject

/**
 * Standalone Google Sign-In integration based on Credential Manager + the
 * Google Identity ID-token API.
 *
 * Important: this class deliberately does NOT touch FirebaseAuth. The Google
 * `userId` (the JWT `sub` claim) is extracted directly from the returned ID
 * token and used as the Firestore document ID under `users/{userId}`.
 *
 * Throws on cancel/failure; callers should surface the message in a toast.
 */
class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Run the Credential Manager flow and return the resolved [UserProfile].
     */
    suspend fun signIn(): UserProfile {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(request = request, context = context)
        val credential = response.credential

        require(credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unsupported credential type: ${credential::class.java.simpleName}"
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleCredential.idToken
        val claims = decodeIdTokenPayload(idToken)
        val userId = claims?.optString("sub").orEmpty()
        require(userId.isNotEmpty()) { "Google ID token missing `sub` claim" }

        val email = googleCredential.id.takeIf { it.isNotBlank() }
            ?: claims?.optString("email").orEmpty()
        val name = googleCredential.displayName ?: claims?.optString("name").orEmpty()
        val photoUrl = googleCredential.profilePictureUri?.toString().orEmpty()
            .ifEmpty { claims?.optString("picture").orEmpty() }

        return UserProfile(
            userId = userId,
            name = name,
            email = email,
            photoUrl = photoUrl
        )
    }

    /**
     * Decode the JWT payload (middle segment) without verifying its signature.
     * The token was just delivered to us by Google's Credential Manager so we
     * trust it; we only need the `sub`/`email`/`name` claims.
     */
    private fun decodeIdTokenPayload(idToken: String): JSONObject? = try {
        val parts = idToken.split(".")
        if (parts.size < 2) null
        else JSONObject(
            String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING),
                Charsets.UTF_8
            )
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decode ID token payload", e)
        null
    }

    companion object {
        // Same OAuth Web Client ID previously hard-coded in the app module.
        // Sourced from the Firebase project; safe to commit (it's not a secret).
        const val WEB_CLIENT_ID = "492953791844-hkrsjm2bvsppofgdidbbjgbke3thuhe6.apps.googleusercontent.com"
        private const val TAG = "GoogleAuthManager"
    }
}
