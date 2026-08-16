package com.appgurjant.stickynotes.ads

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Adaptive bottom banner for high-traffic browse screens.
 *
 * Hidden automatically after Security unlock (ad-free). Collapses to zero height
 * until an ad loads so failed slots never leave an empty gap.
 */
@Composable
fun NoteziaBannerAd(
    modifier: Modifier = Modifier,
    viewModel: BannerAdViewModel = hiltViewModel()
) {
    val showBanner by viewModel.showBanner.collectAsState()
    if (!showBanner) return

    val context = LocalContext.current
    val adWidthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(320)
    var adLoaded by remember { mutableStateOf(false) }

    val adView = remember(adWidthDp) {
        AdView(context).apply {
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
            )
            adUnitId = AdsConfig.bannerAdUnitId
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    DisposableEffect(adView) {
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                adLoaded = true
                Log.d(TAG, "Banner loaded (${AdsConfig.bannerAdUnitId})")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                adLoaded = false
                Log.w(TAG, "Banner failed: ${error.code} ${error.message}")
            }
        }
        adView.loadAd(AdRequest.Builder().build())
        onDispose {
            adView.destroy()
            adLoaded = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (adLoaded) Modifier.wrapContentHeight() else Modifier.height(0.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView.parent?.let { (it as ViewGroup).removeView(adView) }; adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private const val TAG = "NoteziaBannerAd"
