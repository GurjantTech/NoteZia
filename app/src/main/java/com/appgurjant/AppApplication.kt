package com.appgurjant


import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


/**
 * Hilt entry point. Implements [Configuration.Provider] so WorkManager picks
 * up the [HiltWorkerFactory] required by the cloud-sync worker
 * ([com.appgurjant.stickynotes.sync.NotesSyncWorker]).
 */
@HiltAndroidApp
class AppApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    var instance: AppApplication? = null

    init {
        instance = this
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
