package com.appgurjant.stickynotes.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.app.domain.repository.CoinRepository
import com.appgurjant.stickynotes.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdMob entry point for NoteZia:
 * - Rewarded → Settings Premium / Security unlock, Signature Practice
 * - Interstitial → after eligible note deletes
 * - Banner → handled by [NoteziaBannerAd]
 *
 * While Premium is active, [canShowAds] is false and every ad is refused.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val coinRepository: CoinRepository
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val initStarted = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    private val isShowingFullscreen = AtomicBoolean(false)

    private val isLoadingRewarded = AtomicBoolean(false)
    private val isLoadingInterstitial = AtomicBoolean(false)

    @Volatile private var preloadedRewarded: RewardedAd? = null
    @Volatile private var preloadedInterstitial: InterstitialAd? = null
    @Volatile private var activityContext: Activity? = null

    private var consecutiveRewardedFailures = 0
    private var consecutiveInterstitialFailures = 0
    private val completedNoteActions = AtomicInteger(0)
    @Volatile private var lastInterstitialShownAtMs = 0L

    fun canShowAds(): Boolean = coinRepository.areAdsEnabled()

    fun shouldOfferFallback(): Boolean =
        consecutiveRewardedFailures >= MAX_FAILURES_BEFORE_FALLBACK

    fun registerActivity(activity: Activity) {
        activityContext = activity
        if (isInitialized.get()) {
            preloadRewardedAd()
            preloadInterstitialAd()
        }
    }

    fun onActivityDestroyed() {
        activityContext = null
    }

    fun initialize() {
        if (!initStarted.compareAndSet(false, true)) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { initialize() }
            return
        }

        try {
            if (BuildConfig.DEBUG && AdsConfig.testDeviceIds.isNotEmpty()) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        .setTestDeviceIds(AdsConfig.testDeviceIds)
                        .build()
                )
            }

            Log.d(TAG, "Initializing Mobile Ads (debug=${BuildConfig.DEBUG})")
            Log.d(TAG, "Rewarded: ${AdsConfig.rewardedAdUnitId}")
            Log.d(TAG, "Banner: ${AdsConfig.bannerAdUnitId}")
            Log.d(TAG, "Interstitial: ${AdsConfig.interstitialAdUnitId}")

            MobileAds.initialize(appContext) {
                isInitialized.set(true)
                MobileAds.setAppMuted(true)
                MobileAds.setAppVolume(0f)
                Log.d(TAG, "Mobile Ads initialized")
                mainHandler.post {
                    preloadRewardedAd()
                    preloadInterstitialAd()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Mobile Ads initialization failed", t)
            initStarted.set(false)
        }
    }

    // region Rewarded

    fun showRewardedAd(activity: Activity, onResult: (EarnAdResult) -> Unit) {
        if (!canShowAds()) {
            onResult(EarnAdResult.Unavailable)
            return
        }
        if (!isShowingFullscreen.compareAndSet(false, true)) {
            onResult(EarnAdResult.Unavailable)
            return
        }

        val deliverOnce = OneShot<EarnAdResult> { result ->
            isShowingFullscreen.set(false)
            onResult(result)
        }

        mainHandler.post {
            val ready = preloadedRewarded
            if (ready != null) {
                presentRewardedAd(activity, ready, deliverOnce)
            } else {
                loadRewardedAd(
                    onLoaded = { ad -> presentRewardedAd(activity, ad, deliverOnce) },
                    onFailed = { deliverOnce.run(EarnAdResult.Unavailable) }
                )
            }
        }
    }

    fun preloadRewardedAd() {
        if (!canShowAds() || !isInitialized.get()) return
        if (preloadedRewarded != null || isLoadingRewarded.get()) return
        loadRewardedAd(onLoaded = {}, onFailed = {})
    }

    private fun loadRewardedAd(
        onLoaded: (RewardedAd) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!canShowAds() || !isInitialized.get()) {
            onFailed()
            return
        }
        if (preloadedRewarded != null) {
            onLoaded(preloadedRewarded!!)
            return
        }
        if (!isLoadingRewarded.compareAndSet(false, true)) {
            onFailed()
            return
        }

        val loadContext = activityContext ?: appContext
        mainHandler.post {
            RewardedAd.load(
                loadContext,
                AdsConfig.rewardedAdUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        preloadedRewarded = ad
                        isLoadingRewarded.set(false)
                        consecutiveRewardedFailures = 0
                        Log.d(TAG, "Rewarded ad loaded")
                        onLoaded(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        preloadedRewarded = null
                        isLoadingRewarded.set(false)
                        consecutiveRewardedFailures++
                        Log.w(TAG, "Rewarded load failed: ${error.code} ${error.message}")
                        scheduleRewardedBackoff()
                        onFailed()
                    }
                }
            )
        }
    }

    private fun presentRewardedAd(
        activity: Activity,
        ad: RewardedAd,
        deliver: OneShot<EarnAdResult>
    ) {
        if (!canShowAds() || activity.isFinishing || activity.isDestroyed) {
            deliver.run(EarnAdResult.Unavailable)
            return
        }

        val earned = AtomicBoolean(false)
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                preloadedRewarded = null
            }

            override fun onAdDismissedFullScreenContent() {
                preloadedRewarded = null
                preloadRewardedAd()
                deliver.run(if (earned.get()) EarnAdResult.Earned else EarnAdResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded show failed: ${error.message}")
                preloadedRewarded = null
                preloadRewardedAd()
                deliver.run(EarnAdResult.Unavailable)
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            earned.set(true)
        }
    }

    private fun scheduleRewardedBackoff() {
        if (!canShowAds()) return
        if (consecutiveRewardedFailures > MAX_BACKOFF_ATTEMPTS) return
        val delayMs = (1_000L shl (consecutiveRewardedFailures - 1).coerceAtLeast(0))
            .coerceAtMost(MAX_BACKOFF_MS)
        mainHandler.postDelayed({ preloadRewardedAd() }, delayMs)
    }

    // endregion

    // region Interstitial

    /**
     * Call after a successful note delete.
     * Frequency-capped: every [AdsConfig.interstitialEveryNActions] deletes and
     * at least [AdsConfig.interstitialMinIntervalMs] apart.
     * Always invokes [onFinished] once (after dismiss, skip, or failure).
     */
    fun showInterstitialIfEligible(activity: Activity, onFinished: () -> Unit) {
        val finishOnce = OneShot<Unit> { onFinished() }

        if (!canShowAds()) {
            finishOnce.run(Unit)
            return
        }

        val count = completedNoteActions.incrementAndGet()
        val everyN = AdsConfig.interstitialEveryNActions.coerceAtLeast(1)
        val dueByCount = count % everyN == 0
        val elapsed = System.currentTimeMillis() - lastInterstitialShownAtMs
        val dueByTime = lastInterstitialShownAtMs == 0L ||
            elapsed >= AdsConfig.interstitialMinIntervalMs

        if (!dueByCount || !dueByTime) {
            Log.d(TAG, "Interstitial skipped (count=$count, elapsed=${elapsed}ms)")
            finishOnce.run(Unit)
            return
        }

        if (!isShowingFullscreen.compareAndSet(false, true)) {
            finishOnce.run(Unit)
            return
        }

        mainHandler.post {
            val ready = preloadedInterstitial
            if (ready != null) {
                presentInterstitialAd(activity, ready, finishOnce)
            } else {
                loadInterstitialAd(
                    onLoaded = { ad -> presentInterstitialAd(activity, ad, finishOnce) },
                    onFailed = {
                        isShowingFullscreen.set(false)
                        finishOnce.run(Unit)
                    }
                )
            }
        }
    }

    fun preloadInterstitialAd() {
        if (!canShowAds() || !isInitialized.get()) return
        if (preloadedInterstitial != null || isLoadingInterstitial.get()) return
        loadInterstitialAd(onLoaded = {}, onFailed = {})
    }

    private fun loadInterstitialAd(
        onLoaded: (InterstitialAd) -> Unit,
        onFailed: () -> Unit
    ) {
        if (!canShowAds() || !isInitialized.get()) {
            onFailed()
            return
        }
        if (preloadedInterstitial != null) {
            onLoaded(preloadedInterstitial!!)
            return
        }
        if (!isLoadingInterstitial.compareAndSet(false, true)) {
            onFailed()
            return
        }

        val loadContext = activityContext ?: appContext
        mainHandler.post {
            Log.d(
                TAG,
                "Loading interstitial (unit=${AdsConfig.interstitialAdUnitId}, debug=${BuildConfig.DEBUG})"
            )
            InterstitialAd.load(
                loadContext,
                AdsConfig.interstitialAdUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        preloadedInterstitial = ad
                        isLoadingInterstitial.set(false)
                        consecutiveInterstitialFailures = 0
                        Log.d(TAG, "Interstitial ad loaded")
                        onLoaded(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        preloadedInterstitial = null
                        isLoadingInterstitial.set(false)
                        consecutiveInterstitialFailures++
                        Log.w(TAG, "Interstitial load failed: ${error.code} ${error.message}")
                        scheduleInterstitialBackoff()
                        onFailed()
                    }
                }
            )
        }
    }

    private fun presentInterstitialAd(
        activity: Activity,
        ad: InterstitialAd,
        finishOnce: OneShot<Unit>
    ) {
        if (!canShowAds() || activity.isFinishing || activity.isDestroyed) {
            isShowingFullscreen.set(false)
            finishOnce.run(Unit)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                preloadedInterstitial = null
                lastInterstitialShownAtMs = System.currentTimeMillis()
                Log.d(TAG, "Interstitial shown")
            }

            override fun onAdDismissedFullScreenContent() {
                preloadedInterstitial = null
                isShowingFullscreen.set(false)
                preloadInterstitialAd()
                finishOnce.run(Unit)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial show failed: ${error.message}")
                preloadedInterstitial = null
                isShowingFullscreen.set(false)
                preloadInterstitialAd()
                finishOnce.run(Unit)
            }
        }

        ad.show(activity)
    }

    private fun scheduleInterstitialBackoff() {
        if (!canShowAds()) return
        if (consecutiveInterstitialFailures > MAX_BACKOFF_ATTEMPTS) return
        val delayMs = (1_000L shl (consecutiveInterstitialFailures - 1).coerceAtLeast(0))
            .coerceAtMost(MAX_BACKOFF_MS)
        mainHandler.postDelayed({ preloadInterstitialAd() }, delayMs)
    }

    // endregion

    private class OneShot<T>(private val action: (T) -> Unit) {
        private val fired = AtomicBoolean(false)
        fun run(value: T) {
            if (fired.compareAndSet(false, true)) {
                action(value)
            }
        }
    }

    companion object {
        private const val TAG = "AdsManager"
        private const val MAX_BACKOFF_ATTEMPTS = 4
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_FAILURES_BEFORE_FALLBACK = 3
    }
}
