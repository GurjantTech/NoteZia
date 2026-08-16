package com.appgurjant


import android.app.Application
import android.util.Log
import android.webkit.WebView
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.app.domain.repository.AuthRepository
import com.app.domain.usecase.ActivateCloudSyncUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Hilt entry point. Implements [Configuration.Provider] so WorkManager picks
 * up the [HiltWorkerFactory] required by the cloud-sync worker
 * ([com.appgurjant.stickynotes.sync.NotesSyncWorker]).
 *
 * When a signed-in user is available, all local notes for that account are
 * claimed/queued and uploaded to Firestore in the background.
 */
@HiltAndroidApp
class AppApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var activateCloudSyncUseCase: dagger.Lazy<ActivateCloudSyncUseCase>
    @Inject lateinit var authRepository: dagger.Lazy<AuthRepository>
    @Inject lateinit var adsManager: dagger.Lazy<com.appgurjant.stickynotes.ads.AdsManager>

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var instance: AppApplication? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize WebView early to prevent AdMob JavascriptEngine errors
        initializeWebView()
        // Initialize AdMob SDK early; AdsManager preloads rewarded ads after init.
        Log.d(TAG, "Initializing AdMob SDK in Application.onCreate()...")
        adsManager.get().initialize()

        
        // Firebase Auth may restore after process start. When a user becomes
        // available, claim offline notes and sync everything to Firestore.
        applicationScope.launch {
            authRepository.get()
                .observeCurrentUser()
                .map { it?.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) {
                        activateCloudSyncUseCase.get()(userId)
                    }
                }
        }
    }

    /**
     * Initialize WebView in the Application context to ensure it's ready
     * before AdMob tries to use it. This must happen on the main thread.
     */
    private fun initializeWebView() {
        try {
            Log.d(TAG, "Initializing WebView provider in Application...")
            // This triggers WebView factory initialization on the main thread
            WebView.setWebContentsDebuggingEnabled(false)
            Log.d(TAG, "WebView provider initialized successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize WebView provider", t)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        private const val TAG = "AppApplication"
    }
}
