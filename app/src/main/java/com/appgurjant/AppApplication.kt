package com.appgurjant


import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class AppApplication : Application() {
    var instance: AppApplication? = null
    init {
        instance = this
    }
}