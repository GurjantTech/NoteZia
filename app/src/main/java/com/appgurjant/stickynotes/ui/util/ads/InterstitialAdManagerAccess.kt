package com.appgurjant.stickynotes.ui.util.ads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface InterstitialAdEntryPoint {
    fun interstitialAdManager(): InterstitialAdManager
    fun rewardedAdManager(): RewardedAdManager
    fun adsConfig(): AdsConfig
}


@Composable
fun rememberInterstitialAdManager(): InterstitialAdManager {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InterstitialAdEntryPoint::class.java
        ).interstitialAdManager()
    }
}


@Composable
fun rememberRewardedAdManager(): RewardedAdManager {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InterstitialAdEntryPoint::class.java
        ).rewardedAdManager()
    }
}


@Composable
fun rememberAdsConfig(): AdsConfig {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            InterstitialAdEntryPoint::class.java
        ).adsConfig()
    }
}


internal fun interstitialAdManagerFrom(context: Context): InterstitialAdManager =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        InterstitialAdEntryPoint::class.java
    ).interstitialAdManager()
