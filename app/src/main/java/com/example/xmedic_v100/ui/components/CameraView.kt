package com.example.xmedic_v100.ui.components

import android.graphics.PointF
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.xmedic_v100.utils.CaptureQualityEvaluator
import com.example.xmedic_v100.utils.CaptureQualityState
import com.example.xmedic_v100.utils.EvaluatorResult
import com.example.xmedic_v100.utils.ImageProcessor
import com.example.xmedic_v100.utils.QualityLevel
import com.example.xmedic_v100.utils.RadiographCaptureCoordinator
import com.example.xmedic_v100.utils.RadiographClassifier
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    isFlashOn: Boolean = false,
    isAutoCaptureEnabled: Boolean = true,
    triggerCapture: Boolean = false,
    onEvaluatorResult: (EvaluatorResult) -> Unit = {},
    onAutoCaptureRequested: () -> Unit = {},
    onImageCaptured: (Uri) -> Unit = {},
    onCaptureHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val evaluator = remember { CaptureQualityEvaluator() }
    val classifier = remember { RadiographClassifier(context) }
    val coordinator = remember { RadiographCaptureCoordinator() }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var latestCorners by remember { mutableStateOf<List<PointF>?>(null) }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeFrame(
                        imageProxy = imageProxy,
                        evaluator = evaluator,
                        classifier = classifier,
                        coordinator = coordinator,
                        isAutoCaptureEnabled = isAutoCaptureEnabled,
                        onResult = { result, shouldAutoCapture ->
                            latestCorners = result.detectedDocument.corners
                            onEvaluatorResult(result)
                            if (shouldAutoCapture) {
                                onAutoCaptureRequested()
                            }
                        }
                    )
                }
            }
    }

    // Vincular CameraX al ciclo de vida
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraView", "Error al inicializar la cámara", e)
            }
        }, executor)
    }

    // Control de Linterna / Antorcha física mediante CameraControl.enableTorch()
    LaunchedEffect(isFlashOn, camera) {
        val activeCamera = camera
        if (activeCamera != null && activeCamera.cameraInfo.hasFlashUnit()) {
            try {
                activeCamera.cameraControl.enableTorch(isFlashOn)
            } catch (e: Exception) {
                Log.e("CameraView", "Error al activar antorcha/linterna", e)
            }
        }
    }

    // Captura de fotografía de alta resolución
    LaunchedEffect(triggerCapture) {
        if (triggerCapture) {
            val photoFile = File(context.cacheDir, "xray_raw_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        coordinator.onCaptureCompleted()
                        val rawUri = Uri.fromFile(photoFile)
                        val processedUri = ImageProcessor.processAndCacheCapturedImage(context, rawUri, latestCorners) ?: rawUri
                        onImageCaptured(processedUri)
                        onCaptureHandled()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        coordinator.onCaptureFailed()
                        Log.e("CameraView", "Error al capturar imagen", exception)
                        onCaptureHandled()
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            evaluator.reset()
            coordinator.reset()
            classifier.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}

private fun analyzeFrame(
    imageProxy: ImageProxy,
    evaluator: CaptureQualityEvaluator,
    classifier: RadiographClassifier,
    coordinator: RadiographCaptureCoordinator,
    isAutoCaptureEnabled: Boolean,
    onResult: (EvaluatorResult, Boolean) -> Unit
) {
    try {
        val bitmap = imageProxy.toBitmap()
        if (bitmap != null) {
            // 1. Inferencia TFLite con best.tflite
            val aiResult = classifier.classify(bitmap)

            // 2. Evaluación geométrica y de calidad
            val evalResult = evaluator.evaluateFrame(bitmap)
            val qualityAnalysis = ImageProcessor.analyzeBitmapQuality(bitmap)

            // 3. Coordinador de decisiones (TFLite AI tiene prioridad absoluta)
            val decision = coordinator.evaluate(
                aiResult = aiResult,
                openCvDoc = evalResult.detectedDocument,
                quality = qualityAnalysis,
                isAutoCaptureEnabled = isAutoCaptureEnabled
            )

            // 4. Mapeo del resultado para la UI
            val finalEvaluatorResult = if (decision.isAiPositive) {
                evalResult.copy(
                    state = CaptureQualityState.READY,
                    level = QualityLevel.EXCELLENT,
                    guidanceMessage = decision.guidanceMessage,
                    isShutterEnabled = true
                )
            } else {
                evalResult.copy(
                    guidanceMessage = decision.guidanceMessage
                )
            }

            onResult(finalEvaluatorResult, decision.shouldTriggerCapture)
        }
    } catch (_: Exception) {
        // Manejo silencioso de análisis de frames
    } finally {
        imageProxy.close()
    }
}
