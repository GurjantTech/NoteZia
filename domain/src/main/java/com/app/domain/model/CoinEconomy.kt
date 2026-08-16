package com.app.domain.model

object CoinEconomy {
    const val REWARDED_AD_COIN_REWARD = 5

    /** Rewarded ads required to unlock Password & Biometric for this session. */
    const val SECURITY_UNLOCK_ADS_REQUIRED = 2

    /** Ads required to unlock the 24-hour Premium trial. */
    const val PREMIUM_TRIAL_ADS_REQUIRED = 4

    /** Premium trial duration once all required ads are completed. */
    const val PREMIUM_TRIAL_DURATION_MS = 24L * 60L * 60L * 1000L
}
