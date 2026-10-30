package com.example.xmedic_v100.utils

import android.util.Log

enum class CoordinatorCaptureState {
    SEARCHING,
    RADIOGRAPH_DETECTED,
    CAPTURING,
    CAPTURED,
    ERROR
}

data class CoordinatorDecision(
    val state: CoordinatorCaptureState = CoordinatorCaptureState.SEARCHING,
    val guidanceMessage: String = "Buscando radiografía...",
    val shouldTriggerCapture: Boolean = false,
    val isAiPositive: Boolean = false,
    val aiConfidence: Float = 0f,
    val isFramingAcceptable: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Coordinador central que prioriza la clasificación de IA en tiempo real (TFLite)
 * sobre las reglas geométricas rígidas de OpenCV para ejecutar capturas automáticas inmediatas sin retardos artificiales.
 */
class RadiographCaptureCoordinator {

    private var currentState: CoordinatorCaptureState = CoordinatorCaptureState.SEARCHING
    private var isCaptureInProgress: Boolean = false

    /**
     * Evalúa los resultados de TFLite AI, OpenCV y análisis de calidad.
     * La IA tiene PRIORIDAD ABSOLUTA sobre la exigencia de 4 esquinas perfectas de OpenCV.
     */
    fun evaluate(
        aiResult: RadiographClassificationResult,
        openCvDoc: DetectedDocument,
        quality: ImageQualityAnalysis,
        isAutoCaptureEnabled: Boolean
    ): CoordinatorDecision {
        val now = System.currentTimeMillis()

        if (isCaptureInProgress) {
            return CoordinatorDecision(
                state = CoordinatorCaptureState.CAPTURING,
                guidanceMessage = "Capturando...",
                shouldTriggerCapture = false,
                isAiPositive = aiResult.isRadiograph,
                aiConfidence = aiResult.radiographConfidence,
                timestamp = now
            )
        }

        // Caso D & E: IA Negativa (No es radiografía)
        if (!aiResult.isRadiograph) {
            currentState = CoordinatorCaptureState.SEARCHING
            val message = when {
                quality.brightness in 1f..28f -> "Hay poca iluminación. Mejora la luz ambiental"
                quality.brightness > 225f -> "Demasiada iluminación. Reduce la luz directa"
                else -> "Buscando radiografía..."
            }
            return CoordinatorDecision(
                state = CoordinatorCaptureState.SEARCHING,
                guidanceMessage = message,
                shouldTriggerCapture = false,
                isAiPositive = false,
                aiConfidence = aiResult.radiographConfidence,
                timestamp = now
            )
        }

        // Caso C: IA Positiva pero recorte severo o extremo
        if (openCvDoc.isCutOff && openCvDoc.areaRatio > 0.92f) {
            Log.d("XRayScanner", "Coordinador: IA Positiva pero recorte severo. Solicitando ajustar encuadre.")
            return CoordinatorDecision(
                state = CoordinatorCaptureState.SEARCHING,
                guidanceMessage = "Ajusta el encuadre",
                shouldTriggerCapture = false,
                isAiPositive = true,
                aiConfidence = aiResult.radiographConfidence,
                isFramingAcceptable = false,
                timestamp = now
            )
        }

        // Caso A & B: IA Positiva + Encuadre Básico Aceptable
        // (¡NO requiere 4 esquinas perfectas de OpenCV!)
        val framingAcceptable = quality.brightness in 25f..230f && quality.sharpness >= 10f

        if (!framingAcceptable) {
            val warningMsg = if (quality.hasReflections) "Evita los reflejos sobre la imagen" else "La imagen está desenfocada"
            return CoordinatorDecision(
                state = CoordinatorCaptureState.SEARCHING,
                guidanceMessage = warningMsg,
                shouldTriggerCapture = false,
                isAiPositive = true,
                aiConfidence = aiResult.radiographConfidence,
                isFramingAcceptable = false,
                timestamp = now
            )
        }

        // ÉXITO: IA Positiva + Encuadre Aceptable
        // Disparo de captura automática INMEDIATA sin esperas artificiales de 700ms/1000ms
        val shouldTrigger = isAutoCaptureEnabled && !isCaptureInProgress
        if (shouldTrigger) {
            isCaptureInProgress = true
            currentState = CoordinatorCaptureState.CAPTURING
            Log.d("XRayScanner", "Coordinador: DISPARO AUTOMÁTICO INMEDIATO ejecutado. Prob IA=${(aiResult.radiographConfidence * 100).toInt()}%")
        } else {
            currentState = CoordinatorCaptureState.RADIOGRAPH_DETECTED
        }

        return CoordinatorDecision(
            state = if (shouldTrigger) CoordinatorCaptureState.CAPTURING else CoordinatorCaptureState.RADIOGRAPH_DETECTED,
            guidanceMessage = if (shouldTrigger) "Capturando..." else "Radiografía encontrada",
            shouldTriggerCapture = shouldTrigger,
            isAiPositive = true,
            aiConfidence = aiResult.radiographConfidence,
            isFramingAcceptable = true,
            timestamp = now
        )
    }

    fun onCaptureCompleted() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.CAPTURED
        Log.d("XRayScanner", "Coordinador: Captura completada exitosamente.")
    }

    fun onCaptureFailed() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.ERROR
        Log.d("XRayScanner", "Coordinador: Error en captura. Reanudando escáner.")
    }

    fun reset() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.SEARCHING
    }
}
