package com.example.xmedic_v100.utils

import android.content.Context

/**
 * Detector híbrido que combina la IA de TFLite con la detección geométrica de OpenCV.
 * Flujo: TFLite IA (si está disponible) -> Refinamiento geométrico OpenCV -> Evaluación de Calidad.
 */
class HybridRadiographDetector(
    context: Context,
    private val openCvDetector: RadiographDetector = OpenCvRadiographDetector(),
    private val tfliteDetector: RadiographDetector = TfliteRadiographDetector(context)
) : RadiographDetector {

    override suspend fun detect(frame: DetectionFrame): DetectionResult {
        val aiResult = tfliteDetector.detect(frame)
        if (aiResult.isAvailable && aiResult.confidence > 0.60f) {
            return aiResult
        }
        // Fallback a OpenCV geométrico adaptativo
        return openCvDetector.detect(frame)
    }
}
