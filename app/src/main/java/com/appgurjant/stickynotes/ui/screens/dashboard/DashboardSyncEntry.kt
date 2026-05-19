package com.appgurjant.stickynotes.ui.screens.dashboard

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.domain.model.SyncResult
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.showToast
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncEvent
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncViewModel
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import com.appgurjant.stickynotes.ui.util.ads.rememberRewardedAdManager
import androidx.compose.ui.platform.LocalContext

/**
 * Persistent cloud entry point in the dashboard header — mirrors sync state
 * at a glance (signed out / pending count / all synced).
 */
@Composable
fun DashboardSyncHeaderIcon(
    pendingCount: Int,
    isSignedIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.notezyPalette
    val syncedDesc = stringResource(R.string.dashboard_sync_header_synced)
    val pendingDesc = stringResource(R.string.dashboard_sync_header_pending, pendingCount)

    val contentDesc = when {
        !isSignedIn -> stringResource(R.string.note_sync_status_pending)
        pendingCount > 0 -> pendingDesc
        else -> syncedDesc
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(palette.surface)
            .border(1.dp, palette.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            !isSignedIn -> {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = contentDesc,
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            pendingCount > 0 -> {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = palette.brandPrimary
                        ) {
                            Text(
                                text = if (pendingCount > 99) "99+" else pendingCount.toString(),
                                fontSize = 9.sp,
                                fontFamily = FontFamily(Font(R.font.inter_bold)),
                                color = Color.White
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = pendingDesc,
                        tint = palette.brandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            else -> {
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = syncedDesc,
                    tint = palette.successDot.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Wires [CloudSyncViewModel] events for dashboard-triggered sync: routes to
 * Google sign-in when needed, shows the rewarded ad before [syncNow], and
 * surfaces a toast when the merge finishes.
 *
 * Must use the **activity-scoped** [CloudSyncViewModel] so it matches
 * [com.appgurjant.stickynotes.ui.screens.GoogleSignInScreen] and Settings.
 */
@Composable
fun rememberDashboardSyncTrigger(navController: NavController): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current as ComponentActivity
    val cloudSyncViewModel: CloudSyncViewModel = hiltViewModel(activity)
    val rewardedAdManager = rememberRewardedAdManager()

    LaunchedEffect(cloudSyncViewModel, navController) {
        cloudSyncViewModel.events.collect { event ->
            when (event) {
                CloudSyncEvent.SyncRequiresSignIn ->
                    navController.navigate(Screen.GoogleSignInScreen.route)

                CloudSyncEvent.SyncReady -> {
                    val frag = context as? FragmentActivity
                    if (frag != null) {
                        rewardedAdManager.showAd(
                            activity = frag,
                            onProceed = { cloudSyncViewModel.syncNow() }
                        )
                    } else {
                        cloudSyncViewModel.syncNow()
                    }
                }

                is CloudSyncEvent.SyncFinished ->
                    showToast(context, dashboardSyncFinishedMessage(context, event.result))

                else -> Unit
            }
        }
    }

    return remember(cloudSyncViewModel) {
        { cloudSyncViewModel.requestSync() }
    }
}

private fun dashboardSyncFinishedMessage(context: Context, result: SyncResult): String = when {
    result.errorMessage != null -> context.getString(R.string.sync_failed, result.errorMessage)
    result.attempted == 0 -> context.getString(R.string.sync_no_pending)
    result.failed == 0 -> context.getString(R.string.sync_completed)
    else -> context.getString(R.string.sync_partial, result.succeeded, result.attempted)
}
