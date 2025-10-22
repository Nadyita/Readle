package com.readle.app.scanner

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MLKitBarcodeScanner(
    private val context: Context,
    private val onBarcodeDetected: (String) -> Unit
) {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val barcodeScanner = BarcodeScanning.getClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val barcodeDetected = AtomicBoolean(false)

    fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("MLKitBarcodeScanner", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (!barcodeDetected.get()) {
                            for (barcode in barcodes) {
                                if (barcodeDetected.get()) break
                                
                                when (barcode.valueType) {
                                    Barcode.TYPE_ISBN -> {
                                        barcode.rawValue?.let { isbn ->
                                            if (barcodeDetected.compareAndSet(false, true)) {
                                                mainHandler.post {
                                                    onBarcodeDetected(isbn)
                                                }
                                            }
                                        }
                                    }
                                    Barcode.TYPE_TEXT -> {
                                        barcode.rawValue?.let { text ->
                                            if (isValidIsbn(text) && barcodeDetected.compareAndSet(false, true)) {
                                                mainHandler.post {
                                                    onBarcodeDetected(text)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MLKitBarcodeScanner", "Barcode scanning failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        private fun isValidIsbn(text: String): Boolean {
            val cleaned = text.replace("-", "").replace(" ", "")
            return cleaned.length == 10 || cleaned.length == 13
        }
    }
}

