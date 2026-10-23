package com.example.xmedic_v100.utils

import android.graphics.Bitmap

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
    val guidanceMessage: String = "Busca una radiografía dentro del marco",
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
     * Evalúa la imagen en tiempo real y produce un resultado estructurado con el estado de captura.
     */
    fun evaluateFrame(bitmap: Bitmap): EvaluatorResult {
        // 1. Detección geométrica del documento / radiografía
        val doc = detector.detectDocument(bitmap)

        if (doc.confidence < 0.35f || !doc.isQuadrilateral) {
            consecutiveValidFrames = 0
            return EvaluatorResult(
                state = CaptureQualityState.SEARCHING_DOCUMENT,
                guidanceMessage = "No se detecta una radiografía. Colócala frente a la cámara",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (doc.isCutOff) {
            consecutiveValidFrames = 0
            return EvaluatorResult(
                state = CaptureQualityState.OUT_OF_FRAME,
                guidanceMessage = "Asegúrate de mostrar toda la radiografía",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (doc.areaRatio > 0.86f) {
            consecutiveValidFrames = 0
            return EvaluatorResult(
                state = CaptureQualityState.TOO_CLOSE,
                guidanceMessage = "Aleja un poco la cámara",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        if (doc.areaRatio < 0.22f) {
            consecutiveValidFrames = 0
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
            doc.centerOffset.x < -0.22f -> {
                consecutiveValidFrames = 0
                return EvaluatorResult(
                    state = CaptureQualityState.MISALIGNED_LEFT,
                    guidanceMessage = "Mueve la cámara hacia la izquierda",
                    isShutterEnabled = false,
                    detectedDocument = doc,
                    frameQualityState = FrameQualityState.Warning
                )
            }
            doc.centerOffset.x > 0.22f -> {
                consecutiveValidFrames = 0
                return EvaluatorResult(
                    state = CaptureQualityState.MISALIGNED_RIGHT,
                    guidanceMessage = "Mueve la cámara hacia la derecha",
                    isShutterEnabled = false,
                    detectedDocument = doc,
                    frameQualityState = FrameQualityState.Warning
                )
            }
            doc.centerOffset.y < -0.22f -> {
                consecutiveValidFrames = 0
                return EvaluatorResult(
                    state = CaptureQualityState.MISALIGNED_TOP,
                    guidanceMessage = "Mueve la cámara hacia arriba",
                    isShutterEnabled = false,
                    detectedDocument = doc,
                    frameQualityState = FrameQualityState.Warning
                )
            }
            doc.centerOffset.y > 0.22f -> {
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
            return EvaluatorResult(
                state = CaptureQualityState.TOO_DARK,
                guidanceMessage = "Hay poca iluminación. Mejora la luz ambiental",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (qualityAnalysis.brightness > 215f) {
            consecutiveValidFrames = 0
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
            return EvaluatorResult(
                state = CaptureQualityState.GLARE_DETECTED,
                guidanceMessage = "Se detectaron reflejos. Cambia el ángulo de la cámara",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        if (qualityAnalysis.sharpness < 15f) {
            consecutiveValidFrames = 0
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
            return EvaluatorResult(
                state = CaptureQualityState.UNSTABLE,
                guidanceMessage = "Mantén el celular estable",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        // 4. Estabilización temporal: requiere fotogramas válidos consecutivos (~1.5 segundos)
        consecutiveValidFrames++

        return if (consecutiveValidFrames >= 3) {
            EvaluatorResult(
                state = CaptureQualityState.READY,
                guidanceMessage = "Radiografía lista para capturar",
                isShutterEnabled = true,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Acceptable
            )
        } else {
            EvaluatorResult(
                state = CaptureQualityState.VERIFYING,
                guidanceMessage = "Encuadre correcto. Verificando calidad",
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
            avgDiff > 18f
        } catch (_: Exception) {
            false
        }
    }

    fun reset() {
        consecutiveValidFrames = 0
        previousPixels = null
    }
}
