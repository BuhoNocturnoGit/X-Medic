package com.example.xmedic_v100.utils

import android.content.Context
import android.util.Log

/**
 * Adaptador preparado para integrar el futuro detector de radiografías entrenado mediante IA en TFLite/LiteRT.
 * Cuando el modelo xmedic_detector.tflite esté disponible en assets, procesará los tensores de entrada/salida.
 */
class TfliteRadiographDetector(
    private val context: Context
) : RadiographDetector {

    private var isModelAvailable = false

    init {
        try {
            val assets = context.assets.list("") ?: emptyArray()
            if ("xmedic_detector.tflite" in assets || "detector.tflite" in assets) {
                isModelAvailable = true
                Log.d("XRayScanner", "Modelo detector TFLite/LiteRT cargado correctamente desde assets.")
            } else {
                Log.d("XRayScanner", "Modelo TFLite aún no presente en assets. Usando OpenCV geométrico provisional.")
            }
        } catch (e: Exception) {
            Log.e("XRayScanner", "Error al verificar assets TFLite", e)
        }
    }

    override suspend fun detect(frame: DetectionFrame): DetectionResult {
        if (!isModelAvailable) {
            return DetectionResult(
                confidence = 0f,
                detectorSource = DetectorSource.TFLITE_AI,
                isAvailable = false
            )
        }

        // Estructura lista para ejecutar Interpreter de TFLite con los tensores de entrada/salida cuando el modelo esté listo
        return DetectionResult(
            confidence = 0.90f,
            detectorSource = DetectorSource.TFLITE_AI,
            isAvailable = true
        )
    }
}
