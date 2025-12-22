package com.appgurjant.stickynotes.ui.util

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat


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