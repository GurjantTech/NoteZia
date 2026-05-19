package com.appgurjant.stickynotes.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.showToast
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.theme.NotezyAppTheme
import com.appgurjant.stickynotes.ui.theme.ThemeViewModel
import com.appgurjant.stickynotes.ui.util.BannerAd
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncEvent
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncViewModel

/** Light  GoogleSign palette tuned to match marketing reference. */
private object GoogleSignPalette {
    val purplePrimary = Color(0xFFA855F7)
    val purpleDeep = Color(0xFF7C3AED)
    val cyanAccent = Color(0xFF22D3EE)
    val lineLavender = Color(0xFFC4B5FD)
    val lineBlue = Color(0xFF93C5FD)
    val lineMuted = Color(0xFFE2E8F0)
    val headlineInk = Color(0xFF0F172A)
    val subtitleSlate = Color(0xFF64748B)
    val legalMuted = Color(0xFF94A3B8)
    val buttonBorder = Color(0xFFE2E8F0)
    val driveTriangle = Color(0xFF34A853)
    val moonTint = Color(0xFF7C3AED)
    val toggleTrack = Color(0xFFF3E8FF)
}

@Composable
fun GoogleSignInScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val themeViewModel: ThemeViewModel = hiltViewModel(activity)
    val cloudSyncViewModel: CloudSyncViewModel = hiltViewModel(activity)
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val sign_in_success_message = stringResource(R.string.sign_in_success_message)
    LaunchedEffect(Unit) {
        cloudSyncViewModel.events.collect { event ->
            when (event) {
                is CloudSyncEvent.SignInSuccess -> {
                    Log.e("ClaudSync", "SignInSuccess")
                    showToast(context, sign_in_success_message)
                    navController.popBackStack()
                }

                is CloudSyncEvent.SignInFailed -> {
                    Log.e("ClaudSync", "SignInFailed")
                }

                else -> Unit
            }
        }
    }

    GoogleSignInScaffold(
        isDarkTheme = isDarkTheme,
        onThemeToggle = { themeViewModel.setDarkTheme(!isDarkTheme) },
        onGoogleSignIn = { cloudSyncViewModel.signIn() },
        onBackPress = { navController.popBackStack() }
    )
}

@Composable
private fun GoogleSignInScaffold(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onBackPress: () -> Unit
) {
    val scroll = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleSignInBackgroundImage()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                BannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scroll)
                    .padding(20.dp)
            ) {
                GoogleSignInTopBar {
                    onBackPress()
                }
                Spacer(modifier = Modifier.height(20.dp))
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val scale = (maxWidth.value / 360f).coerceIn(0.88f, 1.08f)
                    Column(
                        modifier = Modifier
                            .scale(scale)
                            .widthIn(max = 400.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GoogleSignInHeroIllustration()
                        Spacer(modifier = Modifier.height(36.dp))
                        GoogleSignInHeadline()
                        Spacer(modifier = Modifier.height(14.dp))
                        GoogleSignInSubtitle()
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                GoogleSignInGoogleSignInButton(onClick = onGoogleSignIn)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
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
            modifier = Modifier
                .clickable { onBackPress() }
        )
    }
}

@Composable
private fun GoogleSignInHeroIllustration() {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-58).dp, y = 52.dp)
                .size(56.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(GoogleSignPalette.purplePrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 86.dp, y = (-36).dp)
                .shadow(6.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DriveTriangleMark(edgeSize = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_cloud_sync),
                    color = GoogleSignPalette.headlineInk,
                    fontFamily = FontFamily(Font(R.font.inter_bold)),
                    fontSize = 9.sp,
                    letterSpacing = 1.1.sp
                )
            }
        }

        Surface(
            modifier = Modifier
                .width(152.dp)
                .height(196.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(GoogleSignPalette.lineLavender)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(GoogleSignPalette.lineBlue)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(GoogleSignPalette.lineMuted)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoogleSignPalette.cyanAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveTriangleMark(edgeSize: Dp) {
    Canvas(modifier = Modifier.size(edgeSize)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path, color = GoogleSignPalette.driveTriangle)
    }
}

@Composable
private fun GoogleSignInHeadline() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.sync),
            color = GoogleSignPalette.headlineInk,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 30.sp,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.your_notes),
            color = GoogleSignPalette.purplePrimary,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 34.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoogleSignInSubtitle() {
    Text(
        text = stringResource(R.string.back_up_and_access_your_notes_securely_across_all_your_devices),
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
private fun GoogleSignInGoogleSignInButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
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

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GoogleSignInScreenPreview() {
    NotezyAppTheme(darkTheme = false, dynamicColor = false) {
        GoogleSignInScaffold(
            isDarkTheme = false,
            onThemeToggle = {},
            onGoogleSignIn = {},
            onBackPress = {
            }
        )
    }
}
