package com.appgurjant.stickynotes.ui.screens.qrScanner

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.navigation.NavController
import com.appgurjant.stickynotes.AppUtil.AppEnum
import com.appgurjant.stickynotes.navigation.Screen
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.net.URLEncoder

class QrCodeAnalyzer(val navController: NavController) : ImageAnalysis.Analyzer {
var temp=0
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val img = imageProxy.image
        if (img != null) {
            val inputImage = InputImage.fromMediaImage(img, imageProxy.imageInfo.rotationDegrees)

            // Process image searching for barcodes
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()

            val scanner = BarcodeScanning.getClient(options)
            scanner.process(inputImage)
                .addOnSuccessListener {
                    imageProxy.close()
                    if (!it.isNullOrEmpty()) {
                        for (i in it) {
                            Log.e("QrScannerData", i.rawValue.toString())
                            i.rawValue?.let {
                                if(it.isNotEmpty()){
                                    if(temp==0){
                                        // When screen create this background stack will be removed In Case of qr note
                                        navController.popBackStack(Screen.QrScanScreen.route, true)
                                        navController.navigate(Screen.CreateNewNoteScreen.passNoteType(
                                            AppEnum.QrNote.name,"$it"))
                                        temp++
                                    }

                                }
                            }
                        }
                    }

                }
                .addOnFailureListener {
                    Log.e("QrScannerData: ", "addOnFailureListener : " + it.message)
                }
        }
    }
}