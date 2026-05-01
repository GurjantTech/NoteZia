package com.appgurjant.stickynotes.ui.screens


import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.app.notezy.R
import com.appgurjant.stickynotes.navigation.Screen
import kotlin.io.path.Path

@Composable
fun CustomToolbar(
    title: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp) // toolbar height
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CurvedBottomShape(curveHeight = 40f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.checkbox_on_background), // Replace with your back arrow icon
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = LocalIndication.current
) { onBackClick() }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.weight(1f)) // balance the title
        }
    }
}

class CurvedBottomShape(private val curveHeight: Float = 60f) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ) = androidx.compose.ui.graphics.Outline.Generic(Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, size.height - curveHeight)

        // Curve at bottom
        quadraticBezierTo(
            size.width / 2, size.height + curveHeight,
            size.width, size.height - curveHeight
        )

        lineTo(size.width, 0f)
        close()
    } as Path)
}


@Composable
fun WavyToolbar(
    title: String, navController: NavController
) {
    val waveColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // toolbar height
    ) {
        // Draw Wave Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val path = Path().apply {
                moveTo(0f, 0f) // top-left corner
                lineTo(0f, height * 0.6f)


                // First wave curve
                quadraticBezierTo(
                    width * 0.25f, height,
                    width * 0.5f, height * 0.8f
                )

                // Second wave curve
                quadraticBezierTo(
                    width * 0.75f, height * 0.6f,
                    width, height * 0.8f
                )

                lineTo(width, 0f)
                close()
            }

            drawPath(
                path = path,
                color = waveColor
            )
        }

        // Content: Back button + Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = stringResource(com.appgurjant.stickynotes.R.string.my_notes),
                fontFamily = FontFamily(Font(com.appgurjant.stickynotes.R.font.inter_bold)),
                fontSize = 30.sp,
                color = colorResource(com.appgurjant.stickynotes.R.color.white)
            )

            Image(
                painter = painterResource(com.appgurjant.stickynotes.R.drawable.ic_settings),
                contentDescription = "SettingsIcon",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) {
                    navController.navigate(Screen.SettingScreen.route)
                }
            )
        }

    }
}
@Preview
@Composable
fun TollBarPreview(){
    WavyToolbar("NoteZia", navController = rememberNavController())
}
