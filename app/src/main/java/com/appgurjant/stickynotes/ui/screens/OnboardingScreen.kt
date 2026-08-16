package com.appgurjant.stickynotes.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncEvent
import com.appgurjant.stickynotes.ui.screens.cloudsync.CloudSyncViewModel
import com.appgurjant.stickynotes.ui.screens.onboarding.OnboardingViewModel
import com.appgurjant.stickynotes.ui.theme.NotezyAppTheme
import com.appgurjant.stickynotes.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch
private const val NOTEZIA_LEGAL_URL =
    "https://sites.google.com/view/notezia-privacy-policy?usp=sharing"

/** Light onboarding palette tuned to match marketing reference. */
private object OnboardingPalette {
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
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val themeViewModel: ThemeViewModel = hiltViewModel(activity)
    val cloudSyncViewModel: CloudSyncViewModel = hiltViewModel(activity)
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val noteViewModel: NoteViewModel = hiltViewModel()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    val scope = rememberCoroutineScope()

    // Resolve resource strings inside the @Composable scope so the helper
    // function below can use them without re-querying through `Context`,
    // which lint flags as `LocalContextGetResourceValueCall`.
    val unableToOpenLinkMessage = stringResource(R.string.unable_to_open_link)

    fun openLegalUrl() {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(NOTEZIA_LEGAL_URL)))
        } catch (_: Exception) {
            showToast(context, unableToOpenLinkMessage)
        }
    }

    fun navigateToMainApp() {
        val hasPin = noteViewModel.getPin().isNotBlank()
        val targetRoute = if (hasPin) {
            Screen.AppLockScreen.route
        } else {
            Screen.DashboardScreen.route
        }
        navController.navigate(targetRoute) {
            popUpTo(Screen.OnboardingScreen.route) { inclusive = true }
        }
    }

    fun finishOnboardingAndContinue() {
        scope.launch {
            onboardingViewModel.completeOnboarding()
            navigateToMainApp()
        }
    }

    LaunchedEffect(cloudSyncViewModel) {
        cloudSyncViewModel.events.collect { event ->
            when (event) {
                is CloudSyncEvent.SignInSuccess -> finishOnboardingAndContinue()
                else -> Unit
            }
        }
    }

    OnboardingScaffold(
        isDarkTheme = isDarkTheme,
        onThemeToggle = { themeViewModel.setDarkTheme(!isDarkTheme) },
        onGoogleSignIn = { cloudSyncViewModel.signInWithGoogle() },
        onSkip = { finishOnboardingAndContinue() },
        onTermsClick = { openLegalUrl() },
        onPrivacyClick = { openLegalUrl() }
    )
}

@Composable
private fun OnboardingScaffold(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val scroll = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackgroundImage()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 28.dp)
        ) {
            OnboardingTopBar(
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
            )
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
                    OnboardingHeroIllustration()
                    Spacer(modifier = Modifier.height(36.dp))
                    OnboardingHeadline()
                    Spacer(modifier = Modifier.height(14.dp))
                    OnboardingSubtitle()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OnboardingGoogleSignInButton(onClick = onGoogleSignIn)
            Spacer(modifier = Modifier.height(32.dp))
            OnboardingSkipSignInButton(onClick = onSkip)
            Spacer(modifier = Modifier.height(20.dp))
            OnboardingLegalFooter(
                onTermsClick = onTermsClick,
                onPrivacyClick = onPrivacyClick
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OnboardingBackgroundImage() {
    Image(
        painter = painterResource(R.drawable.onboarding_background),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

@Composable
private fun OnboardingTopBar(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = OnboardingPalette.purplePrimary,
                shadowElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Z",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.inter_bold)),
                        fontSize = 22.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                color = OnboardingPalette.headlineInk,
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 22.sp,
                letterSpacing = (-0.2).sp
            )
        }
//        IconButton(
//            onClick = onThemeToggle,
//            modifier = Modifier
//                .size(44.dp)
//                .clip(CircleShape)
//                .background(OnboardingPalette.toggleTrack)
//        ) {
//            Icon(
//                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
//                contentDescription = stringResource(R.string.cd_theme_toggle),
//                tint = OnboardingPalette.moonTint,
//                modifier = Modifier.size(22.dp)
//            )
//        }
    }
}

@Composable
private fun OnboardingHeroIllustration() {
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
                .background(OnboardingPalette.purplePrimary),
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
                    color = OnboardingPalette.headlineInk,
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
                            .background(OnboardingPalette.lineLavender)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(OnboardingPalette.lineBlue)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(OnboardingPalette.lineMuted)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OnboardingPalette.cyanAccent),
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
        drawPath(path, color = OnboardingPalette.driveTriangle)
    }
}

@Composable
private fun OnboardingHeadline() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.sync),
            color = OnboardingPalette.headlineInk,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 30.sp,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.your_notes),
            color = OnboardingPalette.purplePrimary,
            fontFamily = FontFamily(Font(R.font.inter_bold)),
            fontSize = 34.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingSubtitle() {
    Text(
        text = stringResource(R.string.back_up_and_access_your_notes_securely_across_all_your_devices),
        color = OnboardingPalette.subtitleSlate,
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
private fun OnboardingSkipSignInButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(2.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = OnboardingPalette.purplePrimary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Skip for Now",
                color = OnboardingPalette.toggleTrack,
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}@Composable
private fun OnboardingGoogleSignInButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(2.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OnboardingPalette.buttonBorder)
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
                contentDescription = stringResource(R.string.sign_in_with_google),
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.sign_in_with_google),
                color = OnboardingPalette.headlineInk,
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OnboardingLegalFooter(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val prefix = stringResource(R.string.onboarding_legal_prefix)
    val terms = stringResource(R.string.terms_of_service)
    val and = stringResource(R.string.onboarding_legal_and)
    val privacy = stringResource(R.string.privacy_policy)
    val hScroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = prefix,
            color = OnboardingPalette.legalMuted,
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = terms,
            color = OnboardingPalette.purpleDeep,
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onTermsClick() }
        )
        Text(
            text = and,
            color = OnboardingPalette.legalMuted,
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = privacy,
            color = OnboardingPalette.purpleDeep,
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onPrivacyClick() }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingScreenPreview() {
    NotezyAppTheme(darkTheme = false, dynamicColor = false) {
        OnboardingScaffold(
            isDarkTheme = false,
            onThemeToggle = {},
            onGoogleSignIn = {},
            onSkip = {},
            onTermsClick = {},
            onPrivacyClick = {}
        )
    }
}
