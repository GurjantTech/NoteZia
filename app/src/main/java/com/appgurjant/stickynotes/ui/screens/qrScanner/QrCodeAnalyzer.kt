package com.appgurjant.stickynotes.ui.screens.qrScanner

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.navigation.NavController
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.navigation.Screen
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analyzes camera frames for QR codes using ML Kit.
 *
 * Key design decisions:
 * - Restricts detection to QR_CODE for faster, more reliable scans on dense codes.
 * - Reuses a single [BarcodeScanner] instance instead of recreating it per frame.
 * - Uses [AtomicBoolean] guards to ensure exactly-once navigation and prevent
 *   processing of stale frames after a successful detection.
 * - Always closes the [ImageProxy] via `addOnCompleteListener`, preventing the
 *   camera pipeline from stalling when ML Kit fails or returns empty results.
 */
class QrCodeAnalyzer(
    private val navController: NavController
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    private val isProcessing = AtomicBoolean(false)
    private val hasNavigated = AtomicBoolean(false)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (hasNavigated.get()) {
            imageProxy.close()
            return
        }
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }?.rawValue
                if (!rawValue.isNullOrEmpty() && hasNavigated.compareAndSet(false, true)) {
                    Log.d(TAG, "QR detected: $rawValue")
                    navController.popBackStack(Screen.QrScanScreen.route, true)
                    navController.navigate(
                        Screen.CreateNewNoteScreen.passNoteType(
                            AppEnum.QrNote.name,
                            rawValue
                        )
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Barcode scan failed: ${error.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    companion object {
        private const val TAG = "QrCodeAnalyzer"
    }
}
