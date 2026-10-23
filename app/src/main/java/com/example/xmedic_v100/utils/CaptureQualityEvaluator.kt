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

enum class QualityLevel {
    REJECTED,
    ACCEPTABLE,
    EXCELLENT
}

data class EvaluatorResult(
    val state: CaptureQualityState = CaptureQualityState.SEARCHING_DOCUMENT,
    val level: QualityLevel = QualityLevel.REJECTED,
    val globalScore: Float = 0f,
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
     * Evalúa el fotograma en tiempo real y calcula puntuación flexible y nivel de calidad.
     */
    fun evaluateFrame(bitmap: Bitmap): EvaluatorResult {
        // 1. Análisis de iluminación global
        val qualityAnalysis = ImageProcessor.analyzeBitmapQuality(bitmap)

        if (qualityAnalysis.brightness in 1f..28f) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación: Iluminación insuficiente (Brillo=${qualityAnalysis.brightness})")
            return EvaluatorResult(
                state = CaptureQualityState.TOO_DARK,
                level = QualityLevel.REJECTED,
                globalScore = 20f,
                guidanceMessage = "Hay poca iluminación. Mejora la luz ambiental",
                isShutterEnabled = false,
                detectedDocument = DetectedDocument(),
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (qualityAnalysis.brightness > 225f) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación: Sobreexposición severa (Brillo=${qualityAnalysis.brightness})")
            return EvaluatorResult(
                state = CaptureQualityState.OVEREXPOSED,
                level = QualityLevel.REJECTED,
                globalScore = 25f,
                guidanceMessage = "Demasiada iluminación. Reduce la luz directa",
                isShutterEnabled = false,
                detectedDocument = DetectedDocument(),
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        // 2. Detección geométrica del cuadrilátero de la radiografía
        val doc = detector.detectDocument(bitmap)

        if (doc.confidence < 0.35f || !doc.isQuadrilateral) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación: Buscando radiografía (No se detectó candidato válido)")
            return EvaluatorResult(
                state = CaptureQualityState.SEARCHING_DOCUMENT,
                level = QualityLevel.REJECTED,
                globalScore = 15f,
                guidanceMessage = "Buscando radiografía...",
                isShutterEnabled = false,
                detectedDocument = DetectedDocument(),
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        if (doc.isCutOff) {
            consecutiveValidFrames = 0
            Log.d("XRayScanner", "Evaluación Rechazada: Bordes cortados por el límite del fotograma")
            return EvaluatorResult(
                state = CaptureQualityState.OUT_OF_FRAME,
                level = QualityLevel.REJECTED,
                globalScore = 40f,
                guidanceMessage = "Aleja un poco la cámara para mostrar todos los bordes",
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Insufficient
            )
        }

        // 3. Cálculo del Sistema de Puntuación de Calidad Normalizado (0 .. 100)
        val geometryScore = computeGeometryScore(doc)
        val illuminationScore = computeIlluminationScore(qualityAnalysis)
        val sharpnessScore = computeSharpnessScore(qualityAnalysis.sharpness)
        val isMoving = isCameraMoving(bitmap)
        val stabilityScore = if (isMoving) 30f else 95f

        val globalScore = (ScannerConfig.WEIGHT_GEOMETRY * geometryScore) +
                (ScannerConfig.WEIGHT_ILLUMINATION * illuminationScore) +
                (ScannerConfig.WEIGHT_SHARPNESS * sharpnessScore) +
                (ScannerConfig.WEIGHT_STABILITY * stabilityScore)

        // Clasificación en 3 Niveles de Calidad (REJECTED, ACCEPTABLE, EXCELLENT)
        val level = when {
            geometryScore < ScannerConfig.MIN_GEOMETRY_SCORE ||
                    illuminationScore < ScannerConfig.MIN_ILLUMINATION_SCORE ||
                    sharpnessScore < ScannerConfig.MIN_SHARPNESS_SCORE ||
                    stabilityScore < ScannerConfig.MIN_STABILITY_SCORE -> QualityLevel.REJECTED

            globalScore >= ScannerConfig.SCORE_EXCELLENT_THRESHOLD -> QualityLevel.EXCELLENT
            globalScore >= ScannerConfig.SCORE_ACCEPTABLE_THRESHOLD -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.REJECTED
        }

        if (level == QualityLevel.REJECTED) {
            consecutiveValidFrames = 0
            val rejectedMessage = when {
                qualityAnalysis.hasReflections -> "Evita los reflejos sobre la imagen"
                qualityAnalysis.sharpness < 12f -> "La imagen está borrosa. Enfoca la radiografía"
                isMoving -> "Mantén la cámara estable"
                doc.areaRatio > ScannerConfig.MAX_AREA_RATIO -> "Aleja un poco la cámara"
                doc.areaRatio < ScannerConfig.MIN_AREA_RATIO -> "Acerca un poco la cámara"
                else -> "Ajusta la posición de la radiografía"
            }
            Log.d("XRayScanner", "Evaluación REJECTED: GlobalScore=$globalScore, Geo=$geometryScore, Illum=$illuminationScore, Sharp=$sharpnessScore")
            return EvaluatorResult(
                state = CaptureQualityState.VERIFYING,
                level = QualityLevel.REJECTED,
                globalScore = globalScore,
                guidanceMessage = rejectedMessage,
                isShutterEnabled = false,
                detectedDocument = doc,
                frameQualityState = FrameQualityState.Warning
            )
        }

        // Estabilización temporal suave (~0.8s)
        consecutiveValidFrames++

        val isReady = consecutiveValidFrames >= ScannerConfig.REQUIRED_STABLE_FRAMES

        val finalMessage = when {
            !isReady -> "Verificando los bordes de la radiografía..."
            level == QualityLevel.EXCELLENT -> "Excelente encuadre. Lista para capturar"
            else -> "Imagen aceptable. Puedes capturar"
        }

        Log.d("XRayScanner", "Evaluación ÉXITO: Level=$level, GlobalScore=$globalScore, ShutterEnabled=$isReady, Fotogramas=$consecutiveValidFrames")

        return EvaluatorResult(
            state = if (isReady) CaptureQualityState.READY else CaptureQualityState.VERIFYING,
            level = level,
            globalScore = globalScore,
            guidanceMessage = finalMessage,
            isShutterEnabled = isReady,
            detectedDocument = doc,
            frameQualityState = if (level == QualityLevel.EXCELLENT) FrameQualityState.Acceptable else FrameQualityState.Warning
        )
    }

    private fun computeGeometryScore(doc: DetectedDocument): Float {
        val confPts = (doc.confidence * 40f).coerceIn(0f, 40f)
        val areaPts = if (doc.areaRatio in ScannerConfig.MIN_AREA_RATIO..ScannerConfig.MAX_AREA_RATIO) 30f else 10f
        val centerPts = ((1f - (kotlin.math.abs(doc.centerOffset.x) + kotlin.math.abs(doc.centerOffset.y)) / 2f) * 15f).coerceIn(0f, 15f)
        val tiltPts = if (!doc.isTilted) 15f else 5f
        return (confPts + areaPts + centerPts + tiltPts).coerceIn(0f, 100f)
    }

    private fun computeIlluminationScore(quality: ImageQualityAnalysis): Float {
        val bright = quality.brightness
        if (bright == 0f) return 80f
        val brightScore = ((1f - kotlin.math.abs(bright - 125f) / 125f) * 100f).coerceIn(0f, 100f)
        val reflectionPenalty = if (quality.hasReflections) 25f else 0f
        return (brightScore - reflectionPenalty).coerceIn(0f, 100f)
    }

    private fun computeSharpnessScore(sharpness: Float): Float {
        if (sharpness == 0f) return 80f
        return ((sharpness / 25f) * 100f).coerceIn(0f, 100f)
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
            avgDiff > 22f
        } catch (_: Exception) {
            false
        }
    }

    fun reset() {
        consecutiveValidFrames = 0
        previousPixels = null
    }
}
