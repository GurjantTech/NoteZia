package com.appgurjant.stickynotes.ui.screens.signature

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.ads.rememberAdsManager
import com.appgurjant.stickynotes.ui.theme.notezyPalette

private const val SIGNATURES_BEFORE_REWARDED_AD = 4

/**
 * Practice-only signature pad. Strokes live in memory for this screen only
 * and are never persisted to Room, Firestore, or disk.
 *
 * Every [SIGNATURES_BEFORE_REWARDED_AD] clears (after a drawn signature)
 * shows a rewarded ad, then the counter resets to 0. Opening this screen
 * from Dashboard always starts the counter at 0.
 */
@Composable
fun SignaturePracticeScreen(navController: NavController) {
    val palette = MaterialTheme.notezyPalette
    val adsManager = rememberAdsManager()
    val activity = LocalActivity.current as? Activity

    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    // Not saveable — leaving and re-entering from Dashboard resets to 0.
    var signatureClearCount by remember { mutableIntStateOf(0) }
    var isShowingAd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (adsManager.canShowAds()) {
            adsManager.preloadRewardedAd()
        }
    }

    fun clearPad() {
        strokes = emptyList()
        currentStroke = emptyList()
    }

    fun onClearPressed() {
        val hadSignature = strokes.isNotEmpty() || currentStroke.isNotEmpty()
        clearPad()
        if (!hadSignature || isShowingAd) return

        // Premium / ad-free: practice freely — never prompt for ads.
        if (!adsManager.canShowAds()) {
            signatureClearCount = 0
            return
        }

        val nextCount = signatureClearCount + 1
        if (nextCount < SIGNATURES_BEFORE_REWARDED_AD) {
            signatureClearCount = nextCount
            return
        }

        signatureClearCount = 0
        val host = activity ?: return

        isShowingAd = true
        adsManager.showRewardedAd(host) {
            isShowingAd = false
            signatureClearCount = 0
            if (adsManager.canShowAds()) {
                adsManager.preloadRewardedAd()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBackground)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back),
                    tint = palette.textPrimary
                )
            }
            Text(
                text = stringResource(R.string.signature_practice),
                fontFamily = FontFamily(Font(R.font.inter_bold)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.signature_practice_subtitle_prefix))
                withStyle(
                    SpanStyle(
                        color = palette.brandPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.inter_bold))
                    )
                ) {
                    append(stringResource(R.string.signature_practice_subtitle_emphasis))
                }
            },
            fontFamily = FontFamily(Font(R.font.inter_regular)),
            fontSize = 13.sp,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.surface)
                .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
        ) {
            if (strokes.isEmpty() && currentStroke.isEmpty()) {
                Text(
                    text = stringResource(R.string.signature_practice_hint),
                    color = palette.textMuted,
                    fontSize = 15.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    strokes = strokes + listOf(currentStroke)
                                }
                                currentStroke = emptyList()
                            },
                            onDragCancel = {
                                currentStroke = emptyList()
                            }
                        )
                    }
            ) {
                val strokeStyle = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val ink = Color(0xFF101828)

                fun drawStroke(points: List<Offset>) {
                    if (points.size < 2) {
                        points.firstOrNull()?.let { point ->
                            drawCircle(color = ink, radius = 2.dp.toPx(), center = point)
                        }
                        return
                    }
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(path = path, color = ink, style = strokeStyle)
                }

                strokes.forEach(::drawStroke)
                drawStroke(currentStroke)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onClearPressed() },
                enabled = !isShowingAd,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = palette.textPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.signature_clear),
                    fontFamily = FontFamily(Font(R.font.inter_semibold))
                )
            }
            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brandPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.done),
                    fontFamily = FontFamily(Font(R.font.inter_semibold))
                )
            }
        }
    }
}
