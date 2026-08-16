package com.appgurjant.stickynotes

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.appgurjant.stickynotes.ads.AdsManager
import com.appgurjant.stickynotes.navigation.NoteZyNavGraph
import com.appgurjant.stickynotes.ui.theme.NotezyAppTheme
import com.appgurjant.stickynotes.ui.theme.ThemeViewModel
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    @Inject lateinit var adsManager: AdsManager

    private val updateRequestCode = 100

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showRestartDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            NotezyAppTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                val navController = rememberNavController()
                NoteZyNavGraph(navController)
            }
        }
        
        checkForAppUpdate()
        getFirebaseToken()
        
        // Register Activity context with AdMob after UI is set up
        // AdMob SDK is already initialized in Application.onCreate()
        window.decorView.post {
            Log.d("MainActivity", "Registering Activity with AdMob")
            adsManager.registerActivity(this)
        }
    }

    private fun getFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.e("FirebaseToken:", it)
        }
    }

    private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                appUpdateManager.registerListener(listener)
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    updateRequestCode
                )
            }
        }
    }

    private fun showRestartDialog() {
        if (isFinishing || isDestroyed) return
        try {
            AlertDialog.Builder(this)
                .setTitle("Update Ready")
                .setMessage("The new version has been downloaded. Restart to apply?")
                .setPositiveButton("Restart") { _, _ ->
                    try {
                        appUpdateManager.completeUpdate()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Later", null)
                .setCancelable(false)
                .create()
                .apply {
                    setCanceledOnTouchOutside(false)
                    show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-register Activity in case it was recreated
        adsManager.registerActivity(this)
        adsManager.preloadRewardedAd()
        adsManager.preloadInterstitialAd()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (!isFinishing && !isDestroyed &&
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartDialog()
            }
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(listener)
        adsManager.onActivityDestroyed()
        super.onDestroy()
    }
}
