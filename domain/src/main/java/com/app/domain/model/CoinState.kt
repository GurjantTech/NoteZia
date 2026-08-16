package com.app.domain.model

data class CoinState(
    val balance: Int = 0,
    /** Session unlock via watching ads (resets when process dies). */
    val isSecurityUnlocked: Boolean = false,
    /** Wall-clock expiry for the 24-hour Premium trial (0 = never activated). */
    val premiumExpiresAtMillis: Long = 0L
) {
    val isPremiumActive: Boolean
        get() = premiumExpiresAtMillis > System.currentTimeMillis()

    /** Password & Biometric available this session or while Premium is active. */
    val hasSecurityAccess: Boolean
        get() = isSecurityUnlocked || isPremiumActive

    /** Ad-free only while the 24-hour Premium trial is active. */
    val isAdFree: Boolean get() = isPremiumActive

    val premiumRemainingMs: Long
        get() = (premiumExpiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
}
