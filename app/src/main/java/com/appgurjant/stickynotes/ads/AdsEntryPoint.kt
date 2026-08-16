package com.appgurjant.stickynotes.ads

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
interface AdsManagerEntryPoint {
    fun adsManager(): AdsManager
}

@Composable
fun rememberAdsManager(): AdsManager {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        EntryPointAccessors
            .fromApplication(appContext, AdsManagerEntryPoint::class.java)
            .adsManager()
    }
}

fun Context.adsManager(): AdsManager {
    val app = applicationContext
    return EntryPointAccessors
        .fromApplication(app, AdsManagerEntryPoint::class.java)
        .adsManager()
}
