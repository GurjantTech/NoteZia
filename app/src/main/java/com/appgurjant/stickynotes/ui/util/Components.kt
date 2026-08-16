package com.appgurjant.stickynotes.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.screens.NoteViewModel

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

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun ShowWelcomeNotification(context: Context, noteViewModel: NoteViewModel) {
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
