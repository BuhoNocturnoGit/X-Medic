package com.example.xmedic_v100.utils

import android.graphics.Bitmap
import android.util.Log

enum class CaptureQualityState {
    SEARCHING_DOCUMENT,
    DOCUMENT_NOT_FOUND,
    OUT_OF_FRAME,
    TOO_CLOSE,
    TOO_FAR,
    MISALIGNED_LEFT,
    MISALIGNED_RIGHT,
    MISALIGNED_TOP,
    MISALIGNED_BOTTOM,
    TILTED,
    TOO_DARK,
    OVEREXPOSED,
    GLARE_DETECTED,
    BLUR_DETECTED,
    UNSTABLE,
    VERIFYING,
    READY
}

data class EvaluatorResult(
    val state: CaptureQualityState = CaptureQualityState.SEARCHING_DOCUMENT,
    val guidanceMessage: String = "Buscando radiografía...",
    val isShutterEnabled: Boolean = false,
    val detectedDocument: DetectedDocument = DetectedDocument(),
    val frameQualityState: FrameQualityState = FrameQualityState.Insufficient
)

class CaptureQualityEvaluator(
    private val detector: IXRayDocumentDetector = OpenCVDocumentDetector()
) {
    private var consecutiveValidFrames: Int = 0
    private var previousPixels: IntArray? = null

    /**
     * Evalúa el fotograma en tiempo real y determina las instrucciones y estado del obturador.
     */
    fun evaluateFrame(bitmap: Bitmap): EvaluatorResult {
        // 1. Detección geométrica del cuadrilátero de la radiografía
        val doc = detector.detectDocument(bitmap)

        if (doc.confidence < 0.35f || !doc.isQuadrilateral) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación: Buscando radiografía (Confianza insuficiente)")
            return EvaluatorResult(
                state = CaptureQualityState.SEARCHING_DOCUMENT,
                guidanceMessage = "Buscando radiografía...",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (doc.isCutOff) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Bordes cortados por el límite del fotograma")
            return EvaluatorResult(
                state = CaptureQualityState.OUT_OF_FRAME,
                guidanceMessage = "Aleja un poco la cámara para mostrar todos los bordes",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (doc.areaRatio > 0.88f) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Radiografía demasiado cerca (Área=${(doc.areaRatio * 100).toInt()}%)")
            return EvaluatorResult(
                state = CaptureQualityState.TOO_CLOSE,
                guidanceMessage = "Aleja un poco la cámara",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        if (doc.areaRatio < 0.15f) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Radiografía demasiado lejos (Área=${(doc.areaRatio * 100).toInt()}%)")
            return EvaluatorResult(
                state = CaptureQualityState.TOO_FAR,
                guidanceMessage = "Acerca un poco la cámara",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        // Posicionamiento y desalineación
        when {
            doc.centerOffset.x < -0.28f -> {
                consecutiveValidFrames = 0
                return EvaluatorResult(
                    state = CaptureQualityState.MISALIGNED_LEFT,
                    guidanceMessage = "Mueve la cámara hacia la izquierda",
                    isShutterEnabled = false,
                    detectedDocument = doc,
                    frameQualityState = FrameQualityState.Warning
                )
            }
            doc.centerOffset.x > 0.28f -> {
                consecutiveValidFrames = 0
                return EvaluatorResult(
                    state = CaptureQualityState.MISALIGNED_RIGHT,
                    guidanceMessage = "Mueve la cámara hacia la derecha",
                    isShutterEnabled = false,
                    detectedDocument = doc,
                    frameQualityState = FrameQualityState.Warning
                )
            }
            doc.centerOffset.y < -0.28f -> {
                consecutiveValidFrames = 0
                return EvaluatorResult(
                    state = CaptureQualityState.MISALIGNED_TOP,
                    guidanceMessage = "Mueve la cámara hacia arriba",
                    isShutterEnabled = false,
                    detectedDocument = doc,
                    frameQualityState = FrameQualityState.Warning
                )
            }
            doc.centerOffset.y > 0.28f -> {
                consecutiveValidFrames = 0
                return EvaluatorResult(
                    state = CaptureQualityState.MISALIGNED_BOTTOM,
                    guidanceMessage = "Mueve la cámara hacia abajo",
                    isShutterEnabled = false,
                    detectedDocument = doc,
                    frameQualityState = FrameQualityState.Warning
                )
            }
        }

        if (doc.isTilted) {
            consecutiveValidFrames = 0
            return EvaluatorResult(
                state = CaptureQualityState.TILTED,
                guidanceMessage = "Endereza la cámara",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        // 2. Análisis de Iluminación y Nitidez
        val qualityAnalysis = ImageProcessor.analyzeBitmapQuality(bitmap)

        if (qualityAnalysis.brightness < 35f) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Iluminación insuficiente (Brillo=${qualityAnalysis.brightness})")
            return EvaluatorResult(
                state = CaptureQualityState.TOO_DARK,
                guidanceMessage = "Hay poca iluminación. Mejora la luz ambiental",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (qualityAnalysis.brightness > 220f) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Sobreexposición (Brillo=${qualityAnalysis.brightness})")
            return EvaluatorResult(
                state = CaptureQualityState.OVEREXPOSED,
                guidanceMessage = "Demasiada iluminación. Reduce la luz directa",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (qualityAnalysis.hasReflections) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Reflejos especulares detectados")
            return EvaluatorResult(
                state = CaptureQualityState.GLARE_DETECTED,
                guidanceMessage = "Evita los reflejos sobre la imagen",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        if (qualityAnalysis.sharpness < 12f) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Imagen borrosa (Nitidez=${qualityAnalysis.sharpness})")
            return EvaluatorResult(
                state = CaptureQualityState.BLUR_DETECTED,
                guidanceMessage = "La imagen está borrosa. Enfoca la radiografía",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        // 3. Verificación de estabilidad entre fotogramas
        val isMoving = isCameraMoving(bitmap)
        if (isMoving) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Dispositivo en movimiento")
            return EvaluatorResult(
                state = CaptureQualityState.UNSTABLE,
                guidanceMessage = "Mantén la cámara estable",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        // 4. Estabilización temporal: requiere fotogramas válidos consecutivos (~1.5 segundos)
        consecutiveValidFrames++

        return if (consecutiveValidFrames >= 3) {
            Log.d("XRayScanner", "Evaluación ÉXITO: Radiografía lista para capturar (Fotogramas Válidos=$consecutiveValidFrames)")
            EvaluatorResult(
                state = CaptureQualityState.READY,
                guidanceMessage = "Radiografía lista para capturar",
                isShutterEnabled = true,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Acceptable
            )
        } else {
            Log.d("XRayScanner", "Evaluación Verificando: Estabilizando fotogramas ($consecutiveValidFrames/3)")
            EvaluatorResult(
                state = CaptureQualityState.VERIFYING,
                guidanceMessage = "Radiografía detectada. Mantén la cámara estable",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }
    }

    private fun isCameraMoving(bitmap: Bitmap): Boolean {
        return try {
            val sampleSize = 32
            val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)
            val currentPixels = IntArray(sampleSize * sampleSize)
            scaled.getPixels(currentPixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)

            val prev = previousPixels
            previousPixels = currentPixels

            if (prev == null || prev.size != currentPixels.size) {
                return false
            }

            var diffSum = 0L
            for (i in currentPixels.indices) {
                val p1 = currentPixels[i] and 0xFF
                val p2 = prev[i] and 0xFF
                diffSum += kotlin.math.abs(p1 - p2)
            }

            val avgDiff = diffSum.toFloat() / currentPixels.size
            avgDiff > 20f
        } catch (_: Exception) {
            false
        }
    }

    fun reset() {
        consecutiveValidFrames = 0
        previousPixels = null
    }
}
