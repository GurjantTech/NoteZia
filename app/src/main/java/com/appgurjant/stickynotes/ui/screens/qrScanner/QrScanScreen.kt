package com.appgurjant.stickynotes.ui.screens.qrScanner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.AlertDialogBox
import com.appgurjant.stickynotes.components.takeCameraPermission
import com.appgurjant.stickynotes.ui.theme.notezyPalette
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private object QrScanUiDefaults {
    val frameCornerRadius = 22.dp
    val frameBorderWidth = 3.dp
    val frameSizeMin = 230.dp
    val frameSizeMax = 300.dp
    val topControlsPadding = 16.dp
    val overlayScrimAlpha = 0.56f
    const val frameScreenRatio = 0.68f
}

@Composable
fun QrScanScreen(navController: NavController) {
    var flashMode by remember { mutableStateOf(FLASH_MODE_OFF) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val palette = MaterialTheme.notezyPalette
    val scanAccent = colorResource(R.color.scan_qr_accent)

    takeCameraPermission { granted ->
        if (!granted) showPermissionDialog = true
    }

    Scaffold(
        containerColor = palette.screenBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.pressed } ?: continue
                            val view = previewViewRef ?: continue
                            val control = cameraControl ?: continue
                            val point = view.meteringPointFactory
                                .createPoint(change.position.x, change.position.y)
                            val action = FocusMeteringAction.Builder(
                                point,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            )
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            runCatching { control.startFocusAndMetering(action) }
                        }
                    }
                }
        ) {
            ScannerView(
                navController = navController,
                onCameraReady = { capture, control, view ->
                    imageCapture = capture
                    cameraControl = control
                    previewViewRef = view
                    flashMode = capture.flashMode
                }
            )

            ScannerOverlayView(
                frameColor = scanAccent,
                modifier = Modifier.fillMaxSize()
            )

            QrTopControls(
                isFlashOn = flashMode == FLASH_MODE_ON,
                onFlashToggle = {
                    val newMode = if (flashMode == FLASH_MODE_ON) FLASH_MODE_OFF else FLASH_MODE_ON
                    imageCapture?.flashMode = newMode
                    cameraControl?.enableTorch(newMode == FLASH_MODE_ON)
                    flashMode = newMode
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(QrScanUiDefaults.topControlsPadding)
            )
        }
    }

    if (showPermissionDialog) {
        AlertDialogBox(
            onDismiss = { navController.popBackStack() },
            onConfirm = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }
}

/**
 * Camera preview + ML Kit barcode analysis pipeline.
 *
 * Tuned for reliable detection of dense / high-version QR codes:
 * - [PreviewView.ScaleType.FIT_CENTER] avoids cropping the visible frame.
 * - A high analysis resolution (1280x720+) gives ML Kit enough pixels to
 *   resolve fine modules in dense codes.
 * - An initial center auto-focus locks before the user moves; tap-to-focus
 *   is wired up at the parent so it works through the overlay.
 */
@Composable
private fun ScannerView(
    navController: NavController,
    onCameraReady: (ImageCapture, CameraControl, PreviewView) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val executor = Executors.newSingleThreadExecutor()

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .apply { setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { it.setAnalyzer(executor, QrCodeAnalyzer(navController)) }

                val capture = ImageCapture.Builder()
                    .setFlashMode(FLASH_MODE_OFF)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer,
                        capture
                    )
                    triggerInitialAutoFocus(previewView, camera.cameraControl)
                    onCameraReady(capture, camera.cameraControl, previewView)
                } catch (_: Exception) {
                    // Keep UI stable when camera binding fails (e.g. host is finishing).
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

/** Kicks off a one-shot center focus so the camera locks before the user moves. */
private fun triggerInitialAutoFocus(previewView: PreviewView, control: CameraControl) {
    previewView.post {
        val width = previewView.width.toFloat()
        val height = previewView.height.toFloat()
        if (width <= 0f || height <= 0f) return@post
        val centerPoint = previewView.meteringPointFactory.createPoint(width / 2f, height / 2f)
        val action = FocusMeteringAction.Builder(
            centerPoint,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()
        runCatching { control.startFocusAndMetering(action) }
    }
}

@Composable
private fun ScannerOverlayView(
    frameColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.notezyPalette
    BoxWithConstraints(modifier = modifier) {
        val frameSize = (maxWidth * QrScanUiDefaults.frameScreenRatio)
            .coerceIn(QrScanUiDefaults.frameSizeMin, QrScanUiDefaults.frameSizeMax)
        val corner = QrScanUiDefaults.frameCornerRadius

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = QrScanUiDefaults.overlayScrimAlpha))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(frameSize)
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = QrScanUiDefaults.overlayScrimAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(frameSize)
                        .border(
                            width = QrScanUiDefaults.frameBorderWidth,
                            color = frameColor,
                            shape = RoundedCornerShape(corner)
                        )
                )
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = QrScanUiDefaults.overlayScrimAlpha))
                )
            }
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = QrScanUiDefaults.overlayScrimAlpha))
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Align QR inside the frame",
                color = palette.white,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Auto-detect is enabled",
                color = palette.white.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun QrTopControls(
    isFlashOn: Boolean,
    onFlashToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.36f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFlashToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(if (isFlashOn) R.drawable.flash_on else R.drawable.flash_off),
            contentDescription = "Toggle flash",
            modifier = Modifier.size(24.dp)
        )
    }
}
