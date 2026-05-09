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
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val adsConfig: AdsConfig
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val isLoading = AtomicBoolean(false)
    @Volatile private var preloadedAd: InterstitialAd? = null


    private val sessionCounters = ConcurrentHashMap<String, AtomicInteger>()

    fun loadAd() {
        if (!adsConfig.interstitialAdsEnabled) return
        loadAdIfNeeded()
    }

    fun showAd(activity: Activity, onShown: (() -> Unit)? = null) {
        if (!adsConfig.interstitialAdsEnabled) return
        showAdIfReady(activity, onShown)
    }


    fun showAdEveryN(
        activity: Activity,
        counterKey: String,
        threshold: Int,
        onShown: (() -> Unit)? = null
    ) {
        if (!adsConfig.interstitialAdsEnabled) return
        val counter = sessionCounters.getOrPut(counterKey) { AtomicInteger(0) }
        val newCount = counter.incrementAndGet()
        if (newCount > threshold) {
            showAdIfReady(activity) {
                counter.set(0)
                onShown?.invoke()
            }
        }
    }

    private fun loadAdIfNeeded() {
        if (preloadedAd != null) return
        if (!isLoading.compareAndSet(false, true)) return

        // AdMob requires loads happen on the main thread.
        mainHandler.post {
            InterstitialAd.load(
                appContext,
                adsConfig.interstitialAdUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        preloadedAd = ad
                        isLoading.set(false)
                        Log.d(TAG, "Interstitial loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        preloadedAd = null
                        isLoading.set(false)
                        Log.w(TAG, "Interstitial load failed: ${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Shows the preloaded ad on [activity] if available and the activity is
     * still alive. Triggers preload of the next ad after dismissal.
     */
    private fun showAdIfReady(activity: Activity, onShown: (() -> Unit)? = null) {
        mainHandler.post {
            val ad = preloadedAd
            if (ad == null) {
                // No ad ready: trigger a load for the next opportunity but
                // gracefully skip showing this time per spec.
                loadAdIfNeeded()
                return@post
            }
            if (activity.isFinishing || activity.isDestroyed) {
                Log.d(TAG, "Skipping ad: activity not in a showable state")
                return@post
            }

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    preloadedAd = null
                    onShown?.invoke()
                }

                override fun onAdDismissedFullScreenContent() {
                    preloadedAd = null
                    loadAdIfNeeded()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Interstitial show failed: ${error.message}")
                    preloadedAd = null
                    loadAdIfNeeded()
                }
            }
            ad.show(activity)
        }
    }

    companion object {
        private const val TAG = "InterstitialAdManager"
    }
}
