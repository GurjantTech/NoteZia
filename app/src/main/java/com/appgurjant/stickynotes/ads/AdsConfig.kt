package com.appgurjant.stickynotes.ads

import com.appgurjant.stickynotes.BuildConfig
import com.google.android.gms.ads.AdRequest

/**
 * Build-type AdMob configuration.
 *
 * Debug  → Google sample (test) units
 * Release → live NoteZia units
 */
object AdsConfig {

    val rewardedAdUnitId: String = if (BuildConfig.DEBUG) {
        TEST_REWARDED_AD_UNIT_ID
    } else {
        PROD_REWARDED_AD_UNIT_ID
    }

    val bannerAdUnitId: String = if (BuildConfig.DEBUG) {
        TEST_BANNER_AD_UNIT_ID
    } else {
        PROD_BANNER_AD_UNIT_ID
    }

    val interstitialAdUnitId: String = if (BuildConfig.DEBUG) {
        TEST_INTERSTITIAL_AD_UNIT_ID
    } else {
        PROD_INTERSTITIAL_AD_UNIT_ID
    }

    /**
     * Show an interstitial on every Nth successful note delete.
     * Keeps density AdMob-policy friendly.
     */
    const val interstitialEveryNActions: Int = 5

    /** Minimum gap between two interstitial impressions. */
    const val interstitialMinIntervalMs: Long = 60_000L

    val testDeviceIds: List<String> = if (BuildConfig.DEBUG) {
        listOf(
            AdRequest.DEVICE_ID_EMULATOR,
            "DA877B5E9C0D72755B4E6758311DC742"
        )
    } else {
        emptyList()
    }

    private const val TEST_REWARDED_AD_UNIT_ID =
        "ca-app-pub-3940256099942544/5224354917"
    private const val PROD_REWARDED_AD_UNIT_ID =
        "ca-app-pub-2294761279203706/7165109530"

    private const val TEST_BANNER_AD_UNIT_ID =
        "ca-app-pub-3940256099942544/6300978111"
    private const val PROD_BANNER_AD_UNIT_ID =
        "ca-app-pub-2294761279203706/7556154131"

    private const val TEST_INTERSTITIAL_AD_UNIT_ID =
        "ca-app-pub-3940256099942544/1033173712"
    private const val PROD_INTERSTITIAL_AD_UNIT_ID =
        "ca-app-pub-2294761279203706/2055946024"
}
