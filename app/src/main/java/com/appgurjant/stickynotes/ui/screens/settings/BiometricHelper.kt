package com.appgurjant.stickynotes.ui.screens.settings

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(private val context: Context) {
    fun canAuthenticate(): Int {
        return BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    fun biometricSupportMessage(result: Int): String {
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometric available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Biometric hardware not available on this device."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric sensor is currently unavailable."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprint enrolled. Add one in device settings."
            else -> "Biometric authentication is not supported."
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Authenticate to continue",
        description: String = "Use your fingerprint to unlock",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Use PIN")
            .build()

        val biometricPrompt = getBiometricPrompt(activity, onSuccess, onError)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun getBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(context)
        return BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Keep prompt active; avoid forcing PIN fallback for transient failed scans.
                }
            })
    }

}
