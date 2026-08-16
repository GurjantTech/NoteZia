package com.app.domain.model

/**
 * Outcome of sending a phone verification SMS via Firebase Phone Auth.
 *
 * Most devices receive [CodeSent] and require manual OTP entry. Some devices
 * auto-retrieve the SMS and complete as [AutoVerified] immediately.
 */
sealed interface PhoneVerificationResult {
    data class CodeSent(val verificationId: String) : PhoneVerificationResult
    data class AutoVerified(val profile: UserProfile) : PhoneVerificationResult
}
