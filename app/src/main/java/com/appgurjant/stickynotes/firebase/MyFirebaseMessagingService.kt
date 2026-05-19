package com.appgurjant.stickynotes.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.appgurjant.stickynotes.MainActivity
import com.appgurjant.stickynotes.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e("FirebaseToken",token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

       // If custom data payload exists
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["message"]
            showNotification(title, body)
        }


    }

    private fun showNotification(title: String?, message: String?) {
        val channelId = "default_channel"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                lockscreenVisibility= NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title ?: "NoteZia")
            .setContentText(message ?: "")
            .setSmallIcon(R.drawable.ic_launcher_playstore) // change to your icon
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // ✅ Show on lockscreen
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

}