package com.appgurjant.stickynotes

import android.app.AlertDialog
import android.os.Bundle
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
        // You can replace this with Jetpack Compose AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Update Ready")
            .setMessage("The new version has been downloaded. Restart to apply?")
            .setPositiveButton("Restart") { _, _ ->
                appUpdateManager.completeUpdate()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartDialog()
            }
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(listener)
        super.onDestroy()
    }
}