package com.appgurjant.stickynotes.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.data.PLAY_STORE_APP_URL
import com.appgurjant.stickynotes.BuildConfig
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.showToast
import com.appgurjant.stickynotes.firebase.FirebaseEvent
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.appgurjant.stickynotes.ui.theme.ThemeViewModel
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.util.BannerAd
import com.appgurjant.stickynotes.ui.util.ads.AdCounterKeys
import com.appgurjant.stickynotes.ui.util.ads.rememberAdsConfig
import com.appgurjant.stickynotes.ui.util.ads.rememberInterstitialAdManager
import com.appgurjant.stickynotes.ui.util.ads.rememberRewardedAdManager
import com.appgurjant.stickynotes.ui.screens.settings.SetPinBottomSheet
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.app.domain.model.SyncResult
import com.app.domain.model.UserProfile
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncEvent
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncViewModel

private const val NOTEZIA_PRIVACY_POLICY_URL =
    "https://sites.google.com/view/notezia-privacy-policy?usp=sharing"

@Composable
fun SettingScreen(navController: NavController) {
    val context = LocalContext.current
    val noteViewModel: NoteViewModel = hiltViewModel()
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val cloudSyncViewModel: CloudSyncViewModel = hiltViewModel()
    val interstitialAdManager = rememberInterstitialAdManager()
    val rewardedAdManager = rememberRewardedAdManager()
    val adsConfig = rememberAdsConfig()
    var isOpenBottomSheet by remember { mutableStateOf(false) }
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val isLightTheme = !isDarkTheme
    var passwordEnabled by remember { mutableStateOf(noteViewModel.getPin().isNotBlank()) }
    var biometricEnabled by remember { mutableStateOf(noteViewModel.isFingerprintEnabled()) }
    val currentUser by cloudSyncViewModel.currentUser.collectAsState()
    val isSyncing by cloudSyncViewModel.isSyncing.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Resolve translated strings inside the @Composable scope and pass them
    // into the suspending LaunchedEffect below — `stringResource` cannot be
    // called from inside coroutines, and `context.getString` from there is
    // flagged by lint (LocalContextGetResourceValueCall).
    val signedInAsTemplate = stringResource(R.string.signed_in_as)
    val signInFailedTemplate = stringResource(R.string.sign_in_failed)
    val signedOutMessage = stringResource(R.string.signed_out)

    LaunchedEffect(Unit) {
        cloudSyncViewModel.events.collect { event ->
            when (event) {
                is CloudSyncEvent.SignInSuccess -> showToast(
                    context,
                    signedInAsTemplate.format(event.profile.name.ifBlank { event.profile.email })
                )
                is CloudSyncEvent.SignInFailed -> showToast(
                    context,
                    signInFailedTemplate.format(event.message)
                )
                CloudSyncEvent.SignedOut -> showToast(context, signedOutMessage)
                is CloudSyncEvent.SignOutFailed -> showToast(context, event.message)
                is CloudSyncEvent.SyncFinished -> showToast(context, syncMessage(context, event.result))

                // "Sync with Drive" pre-flight outcomes — see CloudSyncViewModel.requestSync.
                CloudSyncEvent.SyncRequiresSignIn ->
                    navController.navigate(Screen.GoogleSignInScreen.route)
                CloudSyncEvent.SyncReady -> {
                    // Signed in → ad first, then full two-way merge. The
                    // post-sync toast (see syncMessage) handles the
                    // "nothing-to-sync" case when the merge resolves with
                    // attempted = 0.
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        rewardedAdManager.showAd(
                            activity = activity,
                            onProceed = { cloudSyncViewModel.syncNow() }
                        )
                    } else {
                        cloudSyncViewModel.syncNow()
                    }
                }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        biometricEnabled = enabled
        noteViewModel.setFingerprintEnabled(enabled)
    }

    fun handleBiometricToggle(enabled: Boolean) {
        if (!enabled) {
            setBiometricEnabled(false)
            return
        }

        if (noteViewModel.getPin().isBlank()) {
            showToast(context, "Set PIN first to enable biometric authentication.")
            setBiometricEnabled(false)
            return
        }

        val activity = context as? FragmentActivity
        if (activity == null) {
            showToast(context, "Unable to open biometric prompt.")
            setBiometricEnabled(false)
            return
        }

        val biometricHelper = BiometricHelper(context)
        val authSupport = biometricHelper.canAuthenticate()
        if (authSupport != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            showToast(context, biometricHelper.biometricSupportMessage(authSupport))
            setBiometricEnabled(false)
            return
        }

        biometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Enable Biometric Login",
            subtitle = "Confirm your identity",
            description = "Use face or fingerprint based on your device",
            onSuccess = {
                setBiometricEnabled(true)
                showToast(context, "Biometric authentication enabled")
                interstitialAdManager.showAd(activity)
            },
            onError = { error ->
                setBiometricEnabled(false)
                showToast(context, error)
            }
        )
    }

    if (isOpenBottomSheet && !passwordEnabled) {
        SetPinBottomSheet(navController) {
            if (it.isNotEmpty()) {
                isOpenBottomSheet = false
                noteViewModel.setAppPin(it)
                passwordEnabled = true
                (context as? FragmentActivity)?.let {
                    interstitialAdManager.showAd(it)
                }
            }
        }
    }

    SettingScreenUi(
        navController = navController,
        isLightTheme = isLightTheme,
        onThemeSelected = { selectedLight ->
            if (selectedLight != isLightTheme) {
                themeViewModel.setDarkTheme(!selectedLight)
                (context as? FragmentActivity)?.let { activity ->
                    interstitialAdManager.showAdEveryN(
                        activity = activity,
                        counterKey = AdCounterKeys.THEME_CHANGED,
                        threshold = adsConfig.interstitialShowThreshold
                    )
                }
            }
        },
        passwordEnabled = passwordEnabled,
        onPasswordToggle = { enabled ->
            if (enabled) {
                if (noteViewModel.getPin().isBlank()) {
                    isOpenBottomSheet = true
                } else {
                    passwordEnabled = true
                }
            } else {
                passwordEnabled = false
                noteViewModel.setAppPin("")
            }
        },
        biometricEnabled = biometricEnabled,
        onBiometricToggle = { enabled -> handleBiometricToggle(enabled) },
        currentUser = currentUser,
        isSyncing = isSyncing,
        onAccountClick = {
            if (currentUser == null) {
                navController.navigate(Screen.GoogleSignInScreen.route)
            } else {
                showLogoutDialog = true
            }
        },
        onSyncWithDriveClick = {
            // Pre-flight gate. The ViewModel decides whether the user needs
            // to sign in or is ready to run the full two-way merge — we
            // react in the events collector above. The merge itself handles
            // both upload and download, so even a freshly signed-in device
            // with zero local notes still pulls the remote backlog.
            cloudSyncViewModel.requestSync()
        },
        onShareAppClick = {
            FirebaseEvent.logEvent(context, FirebaseEvent.appShareEvent)
            shareApp(context)
        },
        onRateStoreClick = {
            FirebaseEvent.logEvent(context, FirebaseEvent.appRatingEvent)
            openAppInPlayStore(context)
        },
        onChangeLanguageClick = {
            FirebaseEvent.logEvent(context, FirebaseEvent.changeLanguageEvent)
            openLocaleSettings(context)
        },
        onPrivacyClick = { openUrlInBrowser(context, NOTEZIA_PRIVACY_POLICY_URL) },
        onTermsClick = { openUrlInBrowser(context, NOTEZIA_PRIVACY_POLICY_URL) }
    )

    if (showLogoutDialog && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    cloudSyncViewModel.signOut()
                }) { Text(stringResource(R.string.logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun syncMessage(context: Context, result: SyncResult): String = when {
    result.errorMessage != null -> context.getString(R.string.sync_failed, result.errorMessage)
    result.attempted == 0 -> context.getString(R.string.sync_no_pending)
    result.failed == 0 -> context.getString(R.string.sync_completed)
    else -> context.getString(R.string.sync_partial, result.succeeded, result.attempted)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenUi(
    navController: NavController,
    isLightTheme: Boolean,
    onThemeSelected: (Boolean) -> Unit,
    passwordEnabled: Boolean,
    onPasswordToggle: (Boolean) -> Unit,
    biometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    currentUser: UserProfile?,
    isSyncing: Boolean,
    onAccountClick: () -> Unit,
    onSyncWithDriveClick: () -> Unit,
    onShareAppClick: () -> Unit,
    onRateStoreClick: () -> Unit,
    onChangeLanguageClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
) {
    val palette = MaterialTheme.notezyPalette
    Scaffold(
        containerColor = palette.screenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        color = palette.textPrimary,
                        fontFamily = FontFamily(Font(R.font.inter_bold)),
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = palette.textPrimary,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable { navController.popBackStack() }
                    )
                },
                actions = {
                    DriveSyncPill(
                        isSyncing = isSyncing,
                        onClick = onSyncWithDriveClick
                    )
                }
            )
        },
        bottomBar = {
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp)
        ) {
            item { SectionTitle(stringResource(R.string.app_theme), palette.brandPrimary) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeCard(
                        title = stringResource(R.string.light),
                        selected = isLightTheme,
                        isLight = true,
                        onClick = { onThemeSelected(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeCard(
                        title = stringResource(R.string.dark),
                        selected = !isLightTheme,
                        isLight = false,
                        onClick = { onThemeSelected(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.quick_actions), palette.brandPrimary) }
            item {
                QuickActionsRow(
                    onShareAppClick = onShareAppClick,
                    onRateStoreClick = onRateStoreClick,
                    onChangeLanguageClick = onChangeLanguageClick
                )
            }

            item { SectionTitle(stringResource(R.string.security), palette.brandAccent) }
            item {
                SettingsGroupCard {
                    SecurityRow(
                        title = stringResource(R.string.enable_password),
                        subtitle = stringResource(R.string.require_code_to_open_notezia),
                        icon = Icons.Rounded.Lock,
                        iconTint = palette.brandPrimary,
                        enabled = passwordEnabled,
                        onToggle = onPasswordToggle
                    )
                    SecurityRow(
                        title = stringResource(R.string.enable_biometric),
                        subtitle = stringResource(R.string.biometric_unlock_subtitle),
                        icon = Icons.Rounded.Fingerprint,
                        iconTint = palette.brandAccent,
                        enabled = biometricEnabled,
                        onToggle = onBiometricToggle
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.account_data), palette.textMuted) }

            item {
                SettingsGroupCard {
                    GoogleAccountCard(
                        user = currentUser,
                        onClick = onAccountClick
                    )
                }

            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NoteZia v${BuildConfig.VERSION_NAME}",
                        color = palette.textMuted,
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PRIVACY",
                            color = palette.textMuted,
                            fontFamily = FontFamily(Font(R.font.inter_bold)),
                            fontSize = 13.sp,
                            modifier = Modifier.clickable(onClick = onPrivacyClick)
                        )
                        Text("•", color = palette.textMuted)
                        Text(
                            text = "TERMS",
                            color = palette.textMuted,
                            fontFamily = FontFamily(Font(R.font.inter_bold)),
                            fontSize = 13.sp,
                            modifier = Modifier.clickable(onClick = onTermsClick)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(26.dp)) }
        }
    }
}

@Composable
private fun DriveSyncPill(isSyncing: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.notezyPalette
    Row(
        modifier = Modifier
            .background(palette.successBackground, RoundedCornerShape(28.dp))
            .border(1.dp, palette.successBorder, RoundedCornerShape(28.dp))
            .clickable(enabled = !isSyncing, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                color = palette.successDot,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = stringResource(R.string.sync_in_progress),
                color = palette.successText,
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = 10.sp
            )
        } else {
            Icon(Icons.Rounded.Circle, contentDescription = null, tint = palette.successDot, modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.sync_my_notes),
                color = palette.successText,
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily(Font(R.font.inter_bold)),
        fontSize = 16.sp,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
private fun ThemeCard(
    title: String,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.notezyPalette
    val bg = if (isLight) palette.surface else palette.darkSurface
    val border = if (selected) palette.brandPrimary else Color.Transparent
    CardShell(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        bg = bg,
        borderColor = border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if(!isLight) palette.white else palette.textPrimary,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 15.sp
            )
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = palette.brandPrimary, modifier = Modifier.size(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.dp, palette.textSecondary, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    if (isLight) palette.screenBackground else palette.darkSurface,
                    RoundedCornerShape(8.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .height(4.dp)
                    .fillMaxWidth(0.65f)
                    .background(
                        if (isLight) palette.outline else palette.textSecondary,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = MaterialTheme.notezyPalette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(18.dp))
            .padding(vertical = 2.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SecurityRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.brandPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = palette.outline
            )
        )
    }
}

/**
 * Single account row that adapts to sign-in state:
 *  - Logged out: looks like a "Sync with Google" prompt; tap → sign-in screen.
 *  - Logged in: shows the user's name + email + initial-avatar. Tap → logout confirmation.
 * Manual sync runs from the app bar "Sync with Drive" action only.
 */
@Composable
private fun GoogleAccountCard(
    user: UserProfile?,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    CardShell(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        bg = palette.surface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AccountAvatar(user = user)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.name?.takeIf { it.isNotBlank() }
                        ?: user?.email?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.sync_with_google),
                    color = palette.textPrimary,
                    fontFamily = FontFamily(Font(R.font.inter_bold)),
                    fontSize = 14.sp
                )
                Text(
                    text = user?.email?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.cloud_sync_subtitle),
                    color = palette.textSecondary,
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = 12.sp
                )
            }
            if (user != null) {
                Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = palette.textMuted)
            } else {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = palette.textMuted)
            }
        }
    }
}

@Composable
private fun AccountAvatar(user: UserProfile?) {
    val palette = MaterialTheme.notezyPalette
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(palette.screenBackground, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (user == null) {
            Image(
                painter = painterResource(R.drawable.ic_google_g_colored),
                contentDescription = stringResource(R.string.sync_with_google)
            )
        } else {
            Text(
                text = avatarInitials(user),
                color = palette.brandPrimary,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 16.sp
            )
        }
    }
}

private fun avatarInitials(user: UserProfile): String {
    val source = user.name.takeIf { it.isNotBlank() } ?: user.email
    if (source.isBlank()) return "?"
    val parts = source.trim().split(Regex("[ .@]+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].first().uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

@Composable
private fun QuickActionsRow(
    onShareAppClick: () -> Unit,
    onRateStoreClick: () -> Unit,
    onChangeLanguageClick: () -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardShell(
            bg = palette.quickActionsPrimary,
            borderColor = palette.quickActionsPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShareAppClick)
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    palette.quickActionsIconContainer,
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = palette.brandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.share_app),
                        color = palette.textPrimary,
                        fontFamily = FontFamily(Font(R.font.inter_bold)),
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.invite_friends_to_notezia),
                        color = palette.textSecondary,
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionTile(
                modifier = Modifier.weight(1f),
                bg = palette.quickActionsSecondary,
                iconBg = palette.quickActionsIconContainer,
                iconTint = palette.brandAccent,
                icon = Icons.Rounded.Star,
                title = stringResource(R.string.rate_store),
                subtitle = stringResource(R.string.leave_feedback),
                onClick = onRateStoreClick
            )
            QuickActionTile(
                modifier = Modifier.weight(1f),
                bg = palette.quickActionsSecondary,
                iconBg = palette.quickActionsIconContainer,
                iconTint = palette.brandAccent,
                icon = Icons.Rounded.Language,
                title =stringResource(R.string.change_language),
                subtitle = stringResource(R.string.select_your_preferred_language),
                onClick = onChangeLanguageClick
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    bg: Color,
    iconBg: Color,
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    CardShell(
        modifier = modifier
            .wrapContentHeight()
            .clickable(onClick = onClick),
        bg = bg
    ) {
        // CardShell already applies padding; keep inner padding smaller to avoid clipping.
        Column(modifier = Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.padding(top = 5.dp))
            Text(
                text = title,
                color = palette.textPrimary,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 12.sp
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CardShell(
    modifier: Modifier = Modifier,
    bg: Color,
    borderColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(content = content)
    }
}

private fun openLocaleSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(fallbackIntent)
        }
    } catch (_: Exception) {
        // no-op
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {

    }
}

fun openAppInPlayStore(context: Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_APP_URL))
        )
    } catch (e: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_APP_URL))
        )
    }
}

fun shareApp(context: Context) {
    val appLink = PLAY_STORE_APP_URL

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
        putExtra(
            Intent.EXTRA_TEXT,
            "Every idea deserves a secure place. Capture yours with NoteZia.\n\n$appLink"
        )
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share via"))
}

@Composable
@Preview
fun SettingScreenPreview() {
    SettingScreenUi(
        navController = androidx.navigation.compose.rememberNavController(),
        isLightTheme = true,
        onThemeSelected = {},
        passwordEnabled = true,
        onPasswordToggle = {},
        biometricEnabled = false,
        onBiometricToggle = {},
        currentUser = null,
        isSyncing = false,
        onAccountClick = {},
        onSyncWithDriveClick = {},
        onShareAppClick = {},
        onRateStoreClick = {},
        onChangeLanguageClick = {},
        onPrivacyClick = {},
        onTermsClick = {}
    )
}
