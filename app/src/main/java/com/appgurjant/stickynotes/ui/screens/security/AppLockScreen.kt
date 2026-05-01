package com.appgurjant.stickynotes.ui.screens.security

import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.navigation.Screen
import com.appgurjant.stickynotes.ui.screens.NoteViewModel
import com.appgurjant.stickynotes.ui.screens.settings.PinDotsIndicator
import com.appgurjant.stickynotes.ui.screens.settings.PinKeypad
import com.appgurjant.stickynotes.ui.screens.settings.BiometricHelper
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import kotlinx.coroutines.delay

@Composable
fun AppLockScreen(
    navController: NavController
) {
    val noteViewModel: NoteViewModel = hiltViewModel()
    val context = LocalContext.current
    val palette = MaterialTheme.notezyPalette
    val savedPin = remember { noteViewModel.getPin() }
    val fingerprintEnabled = remember { noteViewModel.isFingerprintEnabled() && savedPin.isNotBlank() }
    val pinLength = 4

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinFallback by rememberSaveable { mutableStateOf(!fingerprintEnabled) }
    var biometricHandled by rememberSaveable { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockUntilMillis by remember { mutableLongStateOf(0L) }
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val isLocked = currentMillis < lockUntilMillis
    val lockSecondsLeft = ((lockUntilMillis - currentMillis) / 1000L).coerceAtLeast(0)

    LaunchedEffect(isLocked) {
        if (isLocked) {
            while (currentMillis < lockUntilMillis) {
                delay(1000)
                currentMillis = System.currentTimeMillis()
            }
        }
    }

    LaunchedEffect(fingerprintEnabled, biometricHandled) {
        if (!fingerprintEnabled || biometricHandled) return@LaunchedEffect
        biometricHandled = true
        val activity = context as? FragmentActivity
        if (activity == null) {
            showPinFallback = true
            return@LaunchedEffect
        }
        val helper = BiometricHelper(context)
        val support = helper.canAuthenticate()
        if (support != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            errorMessage = helper.biometricSupportMessage(support)
            showPinFallback = true
            return@LaunchedEffect
        }
        helper.showBiometricPrompt(
            activity = activity,
            title = "Unlock NoteZia",
            subtitle = "Biometric authentication required",
            description = "Use fingerprint to continue",
            onSuccess = {
                navController.navigate(Screen.DashboardScreen.route) {
                    popUpTo(Screen.AppLockScreen.route) { inclusive = true }
                }
            },
            onError = { error ->
                errorMessage = error
                showPinFallback = true
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter PIN",
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.inter_bold)),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLocked) {
                            "Too many attempts. Try again in ${lockSecondsLeft}s."
                        } else {
                            "Enter your 4-digit PIN to unlock NoteZia."
                        },
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily(Font(R.font.inter_regular))
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PinDotsIndicator(
                pinLength = pinLength,
                enteredLength = enteredPin.length,
                filledColor = palette.brandPrimary,
                emptyColor = palette.outline
            )

            if (!errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.inter_semibold))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showPinFallback || !fingerprintEnabled) {
                PinKeypad(
                    onDigit = { digit ->
                        if (isLocked || enteredPin.length >= pinLength) return@PinKeypad
                        enteredPin += digit
                        errorMessage = null
                        if (enteredPin.length == pinLength) {
                            if (enteredPin == savedPin) {
                                navController.navigate(Screen.DashboardScreen.route) {
                                    popUpTo(Screen.AppLockScreen.route) { inclusive = true }
                                }
                            } else {
                                failedAttempts += 1
                                enteredPin = ""
                                errorMessage = "Incorrect PIN. Please try again."
                                if (failedAttempts >= 3) {
                                    lockUntilMillis = System.currentTimeMillis() + 15_000
                                    currentMillis = System.currentTimeMillis()
                                    errorMessage = "Too many attempts. Locked for 15 seconds."
                                    failedAttempts = 0
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (!isLocked && enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                            errorMessage = null
                        }
                    },
                    tint = palette.textPrimary
                )
            } else {
                Text(
                    text = "Waiting for biometric authentication...",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Use PIN instead",
                    color = palette.brandPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    modifier = Modifier.clickable { showPinFallback = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = palette.outline),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .height(52.dp)
                    .clickable(enabled = false) {}
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Locked",
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily(Font(R.font.inter_bold))
                    )
                }
            }
        }
    }
}
