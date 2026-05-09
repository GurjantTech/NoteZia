package com.appgurjant.stickynotes.ui.util.ads

import com.appgurjant.stickynotes.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for ad-related configuration.
 *
 * Values are intentionally hard-coded today, but the surface is shaped like a
 * remote-config object on purpose: when Firebase Remote Config / a feature-flag
 * service is later added, we can swap this class to read from it without
 * touching any caller (e.g. [InterstitialAdManager]).
 *
 * Threshold semantics: an ad is shown *after* the per-counter event count
 * exceeds [interstitialShowThreshold]. e.g. with a value of `2`, the 3rd
 * occurrence of any gated action in a session triggers an interstitial.
 */
@Singleton
class AdsConfig @Inject constructor() {

    /** Master switch for any interstitial ad in the app. */
    val interstitialAdsEnabled: Boolean = true

    /**
     * Interstitial ad unit ID. Google's official test unit is used in DEBUG.
     * Replace [PROD_INTERSTITIAL_AD_UNIT_ID] with your real unit before release.
     */
    val interstitialAdUnitId: String = if (BuildConfig.DEBUG) {
        TEST_INTERSTITIAL_AD_UNIT_ID
    } else {
        PROD_INTERSTITIAL_AD_UNIT_ID
    }

    val interstitialShowThreshold: Int = 2

    /** Master switch for the rewarded ad shown before manual cloud sync. */
    val rewardedAdsEnabled: Boolean = true


    val rewardedAdUnitId: String = if (BuildConfig.DEBUG) {
        TEST_REWARDED_AD_UNIT_ID
    } else {
        PROD_REWARDED_AD_UNIT_ID
    }

    companion object {
        // Google's official test interstitial unit. Safe to use in development.
        private const val TEST_INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/1033173712"

        // TODO: Replace with the real production interstitial unit ID.
        private const val PROD_INTERSTITIAL_AD_UNIT_ID =
            "ca-app-pub-2294761279203706/2055946024"

        // Google's official test rewarded unit. Safe to use in development.
        private const val TEST_REWARDED_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/5224354917"

        // TODO: Replace with the real production rewarded unit ID.
        private const val PROD_REWARDED_AD_UNIT_ID =
            "ca-app-pub-2294761279203706/7165109530"
    }
}
