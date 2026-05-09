package com.appgurjant.stickynotes

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController

import com.appgurjant.stickynotes.navigation.NoteZyNavGraph

import com.appgurjant.stickynotes.ui.theme.NotezyAppTheme
import com.appgurjant.stickynotes.ui.theme.ThemeViewModel
import com.appgurjant.stickynotes.ui.util.ads.InterstitialAdManager
import com.appgurjant.stickynotes.ui.util.ads.RewardedAdManager
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    @Inject lateinit var interstitialAdManager: InterstitialAdManager
    @Inject lateinit var rewardedAdManager: RewardedAdManager

    private val updateRequestCode = 100

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Show snackbar or dialog to restart
            showRestartDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        requestedOrientation =  ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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
        MobileAds.initialize(this) {
            // Mute every full-screen ad (interstitial + rewarded) by default.
            // The GMA SDK requires `initialize()` to complete before these
            // setters can be called; doing it here — and BEFORE preloading —
            // guarantees even the first ad is silent. `setAppVolume(0f)`
            // covers creatives that ignore `setAppMuted`, so together they
            // enforce zero audio output app-wide.
            MobileAds.setAppMuted(true)
            MobileAds.setAppVolume(0f)
            interstitialAdManager.loadAd()
            rewardedAdManager.loadAd()
        }
        checkForAppUpdate()
        getFirebaseToken()



    }


    private fun getFirebaseToken() {
        val token = FirebaseMessaging.getInstance().token
        token.addOnSuccessListener {
            Log.e("FirebaseToken:" ,it)
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
        if (isFinishing || isDestroyed) {
            return  // Don't show dialog if activity is finishing or destroyed
        }
        // You can replace this with Jetpack Compose AlertDialog
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
                .setCancelable(false) // Prevent dismissing by back button
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
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (!isFinishing && !isDestroyed &&
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartDialog()
            }
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(listener)
        super.onDestroy()
    }

}