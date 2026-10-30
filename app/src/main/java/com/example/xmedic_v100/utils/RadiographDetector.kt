package com.example.xmedic_v100.utils

import android.graphics.Bitmap

data class DetectionFrame(
    val bitmap: Bitmap,
    val rotationDegrees: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DetectorSource {
    OPENCV_GEOMETRIC,
    TFLITE_AI,
    HYBRID
}

data class DetectionResult(
    val detectedDocument: DetectedDocument = DetectedDocument(),
    val confidence: Float = 0f,
    val detectorSource: DetectorSource = DetectorSource.OPENCV_GEOMETRIC,
    val timestamp: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true
)

/**
 * Interfaz estandarizada para detectores de radiografías intercambiables.
 * Permite alternar entre OpenCV geométrico, modelo de visión IA en TFLite/LiteRT o un detector híbrido.
 */
interface RadiographDetector {
    suspend fun detect(frame: DetectionFrame): DetectionResult
}
