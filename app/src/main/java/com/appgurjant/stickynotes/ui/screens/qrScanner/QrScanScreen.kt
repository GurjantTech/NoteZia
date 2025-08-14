package com.appgurjant.stickynotes.ui.screens.qrScanner

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.appgurjant.stickynotes.R
import com.appgurjant.stickynotes.components.AlertDialogBox
import com.appgurjant.stickynotes.components.ScanningAnimatedPreloader
import com.appgurjant.stickynotes.components.takeCameraPermission
import com.appgurjant.stickynotes.ui.screens.qrScanner.QrCodeAnalyzer

import java.util.concurrent.Executors

@Composable
fun QrScanScreen(navController: NavController) {
    var flashMode by remember { mutableStateOf(FLASH_MODE_OFF) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var context = LocalContext.current
    Scaffold(
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                takeCameraPermission{ isPermissionGranted->
                    if(!isPermissionGranted){
                        showDialog=true
                    }
                }

                if(showDialog){
                    AlertDialogBox(onDismiss = {
                        navController.popBackStack()
                    },
                        onConfirm = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        }
                    )
                }


                // Camera Preview with flash + control callback
                cameraView(navController) { capture, control ->
                    imageCapture = capture
                    cameraControl = control
                    flashMode = capture.flashMode
                }

                // Flash Toggle Button
                Image(
                    painter = painterResource(
                        if (flashMode == FLASH_MODE_ON) R.drawable.flash_on else R.drawable.flash_off
                    ),
                    contentDescription = "Toggle Flash",

                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .size(40.dp)
                        .clickable {
                            val newMode = if (flashMode == FLASH_MODE_ON)
                                FLASH_MODE_OFF
                            else
                                FLASH_MODE_ON

                            imageCapture?.flashMode = newMode
                            cameraControl?.enableTorch(newMode == FLASH_MODE_ON)
                            flashMode = newMode
                        },
                )

                ScanningAnimatedPreloader(modifier = Modifier.fillMaxWidth())
            }
        }
    )
}



@Composable
fun cameraView(
    navController: NavController,
    onCameraReady: (ImageCapture, CameraControl) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            Executors.newSingleThreadExecutor(),
                            QrCodeAnalyzer(navController)
                        )
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val flashImageCapture = ImageCapture.Builder()
                        .setFlashMode(FLASH_MODE_OFF)
                        .build()

                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer,
                        flashImageCapture
                    )

                    onCameraReady(flashImageCapture, camera.cameraControl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

