package com.appgurjant.stickynotes.ui.screens.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ui.theme.notezyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPinBottomSheet(
    navController: NavController,
    updateUserPin: (String) -> Unit
) {
    val palette = MaterialTheme.notezyPalette
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var enteredPin by rememberSaveable { mutableStateOf("") }
    var firstPin by rememberSaveable { mutableStateOf("") }
    var isConfirmStep by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val pinLength = 4
    val isPinComplete = enteredPin.length == pinLength

    ModalBottomSheet(
        onDismissRequest = { navController.popBackStack() },
        sheetState = bottomSheetState,
        dragHandle = null,
        containerColor = palette.screenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        text = if (isConfirmStep) "Confirm your PIN" else "Create your PIN",
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.inter_bold)),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isConfirmStep) {
                            "Re-enter the same 4-digit code."
                        } else {
                            "Secure your notes with a 4-digit code. This helps keep your private ideas safe."
                        },
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily(Font(R.font.inter_regular)),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
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

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily(Font(R.font.inter_semibold)),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            PinKeypad(
                onDigit = { digit ->
                    if (enteredPin.length < pinLength) {
                        enteredPin += digit
                        errorMessage = null
                    }
                },
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                },
                tint = palette.textPrimary
            )

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPinComplete) palette.brandPrimary else palette.outline
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.96f)
                    .height(52.dp)
                    .clickable(enabled = isPinComplete) {
                        if (!isConfirmStep) {
                            firstPin = enteredPin
                            enteredPin = ""
                            isConfirmStep = true
                            errorMessage = null
                        } else {
                            if (firstPin == enteredPin) {
                                updateUserPin(enteredPin)
                            } else {
                                enteredPin = ""
                                errorMessage = "PINs do not match"
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Confirm",
                        color = if (isPinComplete) palette.white else palette.textSecondary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.inter_bold)),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
