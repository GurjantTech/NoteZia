package com.app.data.auth

import android.app.Activity
import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.app.domain.model.PhoneVerificationResult
import com.app.domain.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Authentication integration for Google + Phone sign-in.
 *
 * Google flow:
 *   Credential Manager → Google ID token → [GoogleAuthProvider] credential
 *   → [FirebaseAuth.signInWithCredential] → Firebase UID.
 *
 * Phone flow:
 *   [PhoneAuthProvider.verifyPhoneNumber] → OTP → Firebase UID.
 *
 * [FirebaseAuth.currentUser] is the single source of truth for session state.
 */
class FirebaseAuthManager(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): UserProfile {
        val idToken = requestGoogleIdToken()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        return result.user?.toUserProfile()
            ?: error("Firebase returned no user after Google sign-in")
    }

    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: Activity
    ): PhoneVerificationResult = suspendCancellableCoroutine { cont ->
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (!cont.isActive) return
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val profile = result.user?.toUserProfile()
                        if (profile != null) {
                            cont.resume(PhoneVerificationResult.AutoVerified(profile))
                        } else {
                            cont.resumeWithException(IllegalStateException("Firebase user missing after auto-verification"))
                        }
                    }
                    .addOnFailureListener { error ->
                        if (cont.isActive) cont.resumeWithException(mapPhoneAuthError(error))
                    }
            }

            override fun onVerificationFailed(error: FirebaseException) {
                if (cont.isActive) cont.resumeWithException(mapPhoneAuthError(error))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                if (cont.isActive) cont.resume(PhoneVerificationResult.CodeSent(verificationId))
            }
        }

        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(normalizePhoneNumber(phoneNumber))
                .setTimeout(PHONE_AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    suspend fun verifyPhoneCode(verificationId: String, code: String): UserProfile {
        val trimmedCode = code.trim()
        require(trimmedCode.length >= 4) { "Invalid verification code" }
        val credential = PhoneAuthProvider.getCredential(verificationId, trimmedCode)
        val result = firebaseAuth.signInWithCredential(credential).await()
        return result.user?.toUserProfile()
            ?: error("Firebase returned no user after phone verification")
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
    }

    fun observeAuthState(): Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUserProfile())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toUserProfile())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun currentUser(): UserProfile? = firebaseAuth.currentUser?.toUserProfile()

    private suspend fun requestGoogleIdToken(): String {
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
        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            "Unsupported credential type: ${credential::class.java.simpleName}"
        }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun normalizePhoneNumber(raw: String): String {
        val cleaned = raw.trim().filter { it.isDigit() || it == '+' }
        require(cleaned.isNotEmpty()) { INVALID_PHONE_NUMBER }

        val region = defaultPhoneRegion()
        val e164 = when {
            cleaned.startsWith("+") -> PhoneNumberUtils.formatNumberToE164(cleaned, region)
            else -> PhoneNumberUtils.formatNumberToE164(cleaned, region)
                ?: PhoneNumberUtils.formatNumberToE164("+$cleaned", region)
        }
        return e164?.takeIf { it.isNotBlank() } ?: error(INVALID_PHONE_NUMBER)
    }

    private fun defaultPhoneRegion(): String {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val iso = sequenceOf(
            telephony?.simCountryIso,
            telephony?.networkCountryIso,
            Locale.getDefault().country
        ).mapNotNull { it?.trim()?.uppercase(Locale.US) }
            .firstOrNull { it.length == 2 }
        return iso ?: "IN"
    }

    private fun mapPhoneAuthError(error: Exception): Exception {
        val raw = error.message.orEmpty()
        val message = when {
            "TOO_SHORT" in raw ||
                "TOO_LONG" in raw ||
                "E.164" in raw ||
                raw.contains("invalid phone number", ignoreCase = true) ||
                raw.contains("format of the phone number", ignoreCase = true) ->
                INVALID_PHONE_NUMBER
            raw.contains("quota", ignoreCase = true) ->
                "Too many attempts. Please try again later."
            raw.contains("expired", ignoreCase = true) ->
                "Verification code expired. Request a new code."
            raw.contains("network", ignoreCase = true) ->
                "Network error. Check your connection and try again."
            raw.contains("invalid verification code", ignoreCase = true) ->
                "Invalid verification code"
            else -> error.message ?: "Phone authentication failed"
        }
        return IllegalStateException(message, error)
    }

    companion object {
        const val WEB_CLIENT_ID =
            "492953791844-hkrsjm2bvsppofgdidbbjgbke3thuhe6.apps.googleusercontent.com"
        const val INVALID_PHONE_NUMBER = "INVALID_PHONE_NUMBER"
        private const val PHONE_AUTH_TIMEOUT_SECONDS = 60L
        private const val TAG = "FirebaseAuthManager"
    }
}

private fun FirebaseUser.toUserProfile() = UserProfile(
    userId = uid,
    name = displayName.orEmpty(),
    email = email.orEmpty(),
    photoUrl = photoUrl?.toString().orEmpty(),
    phoneNumber = phoneNumber.orEmpty()
)
