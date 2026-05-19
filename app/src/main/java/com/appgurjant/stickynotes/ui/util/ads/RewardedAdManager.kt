package com.appgurjant.stickynotes.ui.util.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized rewarded-ad orchestration.
 *
 * The public API is intentionally feature-agnostic — call sites describe the
 * generic operation they want:
 *
 *  - [loadAd]: preload the next ad (typically called once at app start).
 *  - [showAd]: present the preloaded ad, invoking `onProceed` either after
 *    dismissal or — by default — immediately when no ad is available so that
 *    the gated work is never blocked.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val adsConfig: AdsConfig
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val isLoading = AtomicBoolean(false)
    @Volatile private var preloadedAd: RewardedAd? = null

    /**
     * Eagerly load the first rewarded ad. Safe to call multiple times — extra
     * calls are no-ops while a load is already in flight or an ad is ready.
     * Should be invoked once after `MobileAds.initialize(...)`.
     */
    fun loadAd() {
        if (!adsConfig.rewardedAdsEnabled) return
        loadAdIfNeeded()
    }

    /**
     * Show the preloaded rewarded ad and invoke [onProceed] exactly once.
     *
     * @param invokeProceedOnFallback when `true` (default), [onProceed] is
     * still called when no ad is available so the gated operation isn't
     * blocked. Set to `false` for strict show-or-skip semantics.
     */
    fun showAd(
        activity: Activity,
        onProceed: () -> Unit,
        invokeProceedOnFallback: Boolean = true
    ) {
        if (!adsConfig.rewardedAdsEnabled) {
            if (invokeProceedOnFallback) onProceed()
            return
        }
        mainHandler.post {
            val ad = preloadedAd
            if (ad == null) {
                loadAdIfNeeded()
                if (invokeProceedOnFallback) onProceed()
                return@post
            }
            if (activity.isFinishing || activity.isDestroyed) {
                Log.d(TAG, "Skipping rewarded ad: activity not in a showable state")
                if (invokeProceedOnFallback) onProceed()
                return@post
            }

            // Wrap the callback so it can only fire once even if we receive
            // both `onUserEarnedReward` and `onAdDismissedFullScreenContent`.
            val proceedOnce = OneShot(onProceed)

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    preloadedAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    preloadedAd = null
                    loadAdIfNeeded()
                    proceedOnce.run()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Rewarded show failed: ${error.message}")
                    preloadedAd = null
                    loadAdIfNeeded()
                    proceedOnce.run()
                }
            }

            ad.show(activity) {
                proceedOnce.run()
            }
        }
    }

    private fun loadAdIfNeeded() {
        if (preloadedAd != null) return
        if (!isLoading.compareAndSet(false, true)) return

        // AdMob requires loads happen on the main thread.
        mainHandler.post {
            RewardedAd.load(
                appContext,
                adsConfig.rewardedAdUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        preloadedAd = ad
                        isLoading.set(false)
                        Log.d(TAG, "Rewarded loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        preloadedAd = null
                        isLoading.set(false)
                        Log.w(TAG, "Rewarded load failed: ${error.message}")
                    }
                }
            )
        }
    }

    /** Tiny helper that guarantees a callback fires at most once. */
    private class OneShot(private val action: () -> Unit) {
        private val fired = AtomicBoolean(false)
        fun run() {
            if (fired.compareAndSet(false, true)) action()
        }
    }

    companion object {
        private const val TAG = "RewardedAdManager"
    }
}
