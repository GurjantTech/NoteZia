package com.appgurjant.stickynotes.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.GradientBtn
import com.appgurjant.stickynotes.navigation.Screen

@Composable
fun OnboardingScreen(navController: NavController) {
    val noteZiaQuotes = listOf(
        "Idea aaya? NoteZia pe likh daalo!",
        "Chhoti si soch, badi ban sakti hai – bas likh lo!",
        "Kal ka success, aaj ka note ban sakta hai.",
        "Dil aur dimaag ke ideas, sab safe yahan.",
        "Socho mat, likho – baaki NoteZia sambhalega.",
        "Har idea deserves ek jagah – yeh hai woh jagah!",
        "Jab soch naya aaye, turant capture karo.",
        "Likhna hi jeetna hai – start with NoteZia.",
        "Apne thoughts ko action mein badlo.",
        "Yahan se shuru hoti hai tumhari idea ki kahani."
    )

    // Store the random quote in remember to prevent recomposition changes
    val randomQuote = remember { noteZiaQuotes.random() }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = colorResource(R.color.white))
    ) {
        Column(modifier = Modifier
            .padding(20.dp)
            .systemBarsPadding())
        {
            Text(
                stringResource(R.string.app_name),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 40.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                       colors = listOf(
                           Color(0xFF0171FF), // Vibrant Blue
                           Color(0xFF6EC1FF)  // Light Sky Blue
                       )
                    )
                )
            )

            Text(
                randomQuote,
                fontFamily = FontFamily(Font(R.font.inter_semibold)),
                modifier = Modifier.padding(top = 20.dp),
                style = TextStyle(
                    fontSize = 25.sp,
                    lineHeight = 28.sp // Increases space between lines
                )

            )

        }


        // Image aligned BottomEnd
        Image(
            painter = painterResource(R.drawable.ic_onboarding),
            contentDescription = "onBoardingImage",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .matchParentSize(), // Set appropriate size
            contentScale = ContentScale.Fit
        )

        // Gradient Button aligned BottomEnd over the Image
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 20.dp, vertical = 50.dp)
        ) {
            GradientBtn(stringResource(R.string.continue_txt)){
                Log.e("gurjantTrack","Continue")
                navController.popBackStack(Screen.OnboardingScreen.route, true)
                navController.navigate("dashboard")
            }
        }
    }
}



@Preview
@Composable
fun PreviewOnboardingScreen() {
    OnboardingScreen(rememberNavController())
}