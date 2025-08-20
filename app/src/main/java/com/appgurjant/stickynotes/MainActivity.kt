package com.appgurjant.stickynotes

import android.Manifest
import android.app.AlertDialog
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController

import com.appgurjant.stickynotes.navigation.NoteZyNavGraph

import com.appgurjant.stickynotes.ui.theme.NotezyAppTheme
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

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
        setContent {
            NotezyAppTheme{
                val navController = rememberNavController()
                NoteZyNavGraph(navController)
            }
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