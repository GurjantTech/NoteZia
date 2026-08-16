package com.appgurjant.stickynotes.ui.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.domain.model.UserProfile
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.showToast
import com.appgurjant.stickynotes.ui.screens.cloudsync.AuthInProgress
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncEvent
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncViewModel
import com.appgurjant.stickynotes.ui.theme.NotezyAppTheme
import com.appgurjant.stickynotes.ui.theme.ThemeViewModel

private object GoogleSignPalette {
    val purplePrimary = Color(0xFFA855F7)
    val headlineInk = Color(0xFF0F172A)
    val subtitleSlate = Color(0xFF64748B)
    val buttonBorder = Color(0xFFE2E8F0)
}

@Composable
fun GoogleSignInScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val themeViewModel: ThemeViewModel = hiltViewModel(activity)
    val cloudSyncViewModel: CloudSyncViewModel = hiltViewModel(activity)
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val currentUser by cloudSyncViewModel.currentUser.collectAsState()
    val authInProgress by cloudSyncViewModel.authInProgress.collectAsState()
    val signInSuccessMessage = stringResource(R.string.sign_in_success_message)
    val signedOutMessage = stringResource(R.string.signed_out)

    LaunchedEffect(Unit) {
        cloudSyncViewModel.events.collect { event ->
            when (event) {
                is CloudSyncEvent.SignInSuccess -> showToast(context, signInSuccessMessage)
                is CloudSyncEvent.SignedOut -> showToast(context, signedOutMessage)
                is CloudSyncEvent.SignInFailed -> {
                    Log.e("Auth", "SignInFailed: ${event.message}")
                    showToast(context, event.message)
                }
                else -> Unit
            }
        }
    }

    GoogleSignInScaffold(
        isDarkTheme = isDarkTheme,
        currentUser = currentUser,
        isGoogleAuthInProgress = authInProgress == AuthInProgress.Google,
        onGoogleSignIn = { cloudSyncViewModel.signInWithGoogle() },
        onLogout = cloudSyncViewModel::signOut,
        onBackPress = { navController.popBackStack() }
    )
}

@Composable
private fun GoogleSignInScaffold(
    isDarkTheme: Boolean,
    currentUser: UserProfile?,
    isGoogleAuthInProgress: Boolean,
    onGoogleSignIn: () -> Unit,
    onLogout: () -> Unit,
    onBackPress: () -> Unit
) {
    val scroll = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleSignInBackgroundImage()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp)
                ) {
                    GoogleSignInTopBar(onBackPress)
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = maxHeight)
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GoogleSignInHeadline()
                        Spacer(modifier = Modifier.height(14.dp))
                        if (currentUser != null) {
                            Text(
                                text = stringResource(R.string.signed_in_subtitle),
                                color = GoogleSignPalette.subtitleSlate,
                                fontFamily = FontFamily(Font(R.font.inter_regular)),
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            SignedInAccountSection(
                                user = currentUser,
                                onLogoutClick = { showLogoutDialog = true }
                            )
                        } else {
                            GoogleSignInSubtitle()
                            Spacer(modifier = Modifier.height(28.dp))
                            GoogleSignInGoogleSignInButton(
                                enabled = !isGoogleAuthInProgress,
                                loading = isGoogleAuthInProgress,
                                onClick = onGoogleSignIn
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun GoogleSignInBackgroundImage() {
    Image(
        painter = painterResource(R.drawable.onboarding_background),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

@Composable
private fun GoogleSignInTopBar(onBackPress: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            contentDescription = "Back",
            tint = GoogleSignPalette.headlineInk,
            modifier = Modifier.clickable { onBackPress() }
        )
    }
}

@Composable
private fun SignedInAccountSection(
    user: UserProfile,
    onLogoutClick: () -> Unit
) {
    val fallbackName = stringResource(R.string.account_fallback_name)
    val nameLine = user.name.takeIf { it.isNotBlank() }
        ?: user.email.takeIf { it.isNotBlank() }
        ?: user.phoneNumber.takeIf { it.isNotBlank() }
        ?: fallbackName
    val emailLine = user.email.takeIf { it.isNotBlank() && it != nameLine }
    val phoneLine = user.phoneNumber.takeIf { it.isNotBlank() && it != nameLine }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, GoogleSignPalette.buttonBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = accountInitials(nameLine),
                color = GoogleSignPalette.purplePrimary,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 28.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = nameLine,
            color = GoogleSignPalette.headlineInk,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
        if (emailLine != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = emailLine,
                color = GoogleSignPalette.subtitleSlate,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
        if (phoneLine != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = phoneLine,
                color = GoogleSignPalette.subtitleSlate,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "UID: ${user.userId}",
            color = GoogleSignPalette.subtitleSlate,
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Surface(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(2.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            border = BorderStroke(1.dp, GoogleSignPalette.buttonBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = null,
                    tint = GoogleSignPalette.headlineInk,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.logout),
                    color = GoogleSignPalette.headlineInk,
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun accountInitials(source: String): String {
    val parts = source.trim().split(Regex("[ .@+]+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].first().uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

@Composable
private fun GoogleSignInHeadline() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.app_name),
            color = GoogleSignPalette.headlineInk,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 30.sp,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoogleSignInSubtitle() {
    Text(
        text = stringResource(R.string.dashboard_sync_banner_subtitle),
        color = GoogleSignPalette.subtitleSlate,
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    )
}

@Composable
private fun GoogleSignInGoogleSignInButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(2.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GoogleSignPalette.buttonBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    color = GoogleSignPalette.purplePrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google_g_colored),
                        contentDescription = stringResource(R.string.continue_with_google),
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.continue_with_google),
                        color = GoogleSignPalette.headlineInk,
                        fontFamily = FontFamily(Font(R.font.inter_semibold)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GoogleSignInScreenPreview() {
    NotezyAppTheme(darkTheme = false, dynamicColor = false) {
        GoogleSignInScaffold(
            isDarkTheme = false,
            currentUser = null,
            isGoogleAuthInProgress = false,
            onGoogleSignIn = {},
            onLogout = {},
            onBackPress = {}
        )
    }
}
