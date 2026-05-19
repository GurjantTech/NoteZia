package com.appgurjant.stickynotes.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
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
import androidx.navigation.compose.rememberNavController
import com.appgurjant.stickynotes.BuildConfig
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val palette = MaterialTheme.notezyPalette
    val splashViewModel: SplashViewModel = hiltViewModel()
    val noteViewModel: NoteViewModel = hiltViewModel()

    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        delay(2500)

        val onboardingCompleted = splashViewModel.isOnboardingCompleted()
        val targetRoute = when {
            !onboardingCompleted -> Screen.OnboardingScreen.route
            noteViewModel.getPin().isNotBlank() -> Screen.AppLockScreen.route
            else -> Screen.DashboardScreen.route
        }
        navController.navigate(targetRoute) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background( color = colorResource(R.color.white))
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value)
        )
        {

            Image(
                painter = painterResource(R.drawable.ic_launcher_playstore),
                contentDescription = "App logo",
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = palette.brandPrimary
            )
        }
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen(rememberNavController())
}