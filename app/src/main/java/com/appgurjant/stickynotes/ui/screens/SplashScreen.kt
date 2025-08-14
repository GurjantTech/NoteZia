package com.appgurjant.stickynotes.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.BuildCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.appgurjant.stickynotes.BuildConfig
import com.appgurjant.stickynotes.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController:NavController) {

    LaunchedEffect(Unit) {
        delay(2500)
        navController.navigate("onboarding"){
            popUpTo("splash"){inclusive =true}
        }
    }
Box(modifier = Modifier.fillMaxSize()
    .background(color = colorResource(R.color.primary)),
    contentAlignment = Alignment.BottomCenter){
    Column(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    )
    {
        Box (modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center){
            Image(painter = painterResource(R.drawable.app_logo_without_bg),
                contentDescription = "App logo",
                modifier = Modifier.size(160.dp)
            )
        }
        Text(
            stringResource(R.string.tagline2),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(),
            fontFamily = FontFamily(Font(R.font.inter_semibold)),
            fontSize = 20.sp,
            color = colorResource(R.color.white) // White color for the text,

        )
    }
    Text(
        "App Version "+ BuildConfig.VERSION_NAME,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth().padding(bottom = 40.dp),
        fontFamily = FontFamily(Font(R.font.inter_regular)),
        fontSize = 12.sp,
        color = Color.White // White color for the text, ,

    )
}

}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen(rememberNavController())
}