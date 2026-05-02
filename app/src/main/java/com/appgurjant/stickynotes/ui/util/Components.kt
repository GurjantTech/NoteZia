package com.appgurjant.stickynotes.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import com.appgurjant.stickynotes.BuildConfig

import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView


@SuppressLint("ContextCastToActivity")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetStatusBarColor(
    color: androidx.compose.ui.graphics.Color,
    darkIcons: Boolean
) {
    val activity = LocalContext.current as Activity

    SideEffect {
        activity.window.statusBarColor = color.toArgb()
        WindowCompat.getInsetsController(
            activity.window,
            activity.window.decorView
        ).isAppearanceLightStatusBars = darkIcons
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).padding(horizontal = 10.dp),
        factory = { context ->
            /*AdView(context).apply {
                // Set the ad size (e.g., BANNER)
                setAdSize(AdSize.BANNER)
                // Use a test ad unit ID during development
                if(BuildConfig.DEBUG){
//                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                    adUnitId = "ca-app-pub-2294761279203706/7556154131"
                }else{
                    adUnitId = "ca-app-pub-2294761279203706/7556154131"
                }

                // Load the ad
                loadAd(AdRequest.Builder().build()).let { it->
                    Log.e("AdsInfo",it.toString())
                }
            }*/

            val adView = AdView(context)

            val displayMetrics = context.resources.displayMetrics
            val adWidthPixels = displayMetrics.widthPixels
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()

            adView.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    adWidth
                )
            )
            if(BuildConfig.DEBUG){
                adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
            }else{
                adView.adUnitId = "ca-app-pub-2294761279203706/7556154131"
            }

            adView.loadAd(
                AdRequest.Builder().build()
            )

            adView
        }
    )
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun ShowWelcomeNotification(context: Context, noteViewModel: NoteViewModel) {
    // First, update the flag to prevent showing it again
    noteViewModel.setWelcomeNotificationShown(false)
    val channelId = "welcome_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Welcome Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_playstore)
        .setContentTitle("Welcome to NoteZia 🎉")
        .setContentText("Thanks for installing! Let’s get started.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()
    NotificationManagerCompat.from(context).notify(1002, notification)
}