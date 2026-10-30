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
 * sobre las reglas geométricas rígidas de OpenCV para ejecutar capturas automáticas inmediatas sin retrasos artificiales.
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
            Log.d("XMEDIC_AI", "COORDINADOR: Captura en curso (isCaptureInProgress=true). Bloqueando re-disparo.")
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
                quality.brightness in 1f..20f -> "Hay poca iluminación. Mejora la luz ambiental"
                quality.brightness > 235f -> "Demasiada iluminación. Reduce la luz directa"
                else -> "Buscando radiografía..."
            }
            Log.d("XMEDIC_AI", "COORDINADOR: IA Negativa (Prob=${(aiResult.radiographConfidence * 100).toInt()}%). Estado: SEARCHING ($message)")
            return CoordinatorDecision(
                state = CoordinatorCaptureState.SEARCHING,
                guidanceMessage = message,
                shouldTriggerCapture = false,
                isAiPositive = false,
                aiConfidence = aiResult.radiographConfidence,
                timestamp = now
            )
        }

        // Caso C: IA Positiva pero recorte extremo (pantalla pegada al borde sin objeto visible)
        if (openCvDoc.isCutOff && openCvDoc.areaRatio > 0.96f) {
            Log.d("XMEDIC_AI", "COORDINADOR: IA Positiva pero recorte extremo (Área > 96%). Solicitando ajustar encuadre.")
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
        val framingAcceptable = quality.brightness in 15f..240f

        if (!framingAcceptable) {
            val warningMsg = if (quality.brightness < 15f) "Hay poca iluminación. Mejora la luz ambiental" else "Demasiada iluminación"
            Log.d("XMEDIC_AI", "COORDINADOR: IA Positiva pero encuadre inaceptable (Brillo=${quality.brightness}). Message=$warningMsg")
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

        // ÉXITO: IA Positiva + Encuadre Aceptable -> Disparo Inmediato
        val shouldTrigger = isAutoCaptureEnabled && !isCaptureInProgress
        if (shouldTrigger) {
            isCaptureInProgress = true
            currentState = CoordinatorCaptureState.CAPTURING
            Log.d(
                "XMEDIC_AI",
                "COORDINADOR ÉXITO: ¡RADIOGRAFÍA ENCONTRADA! Executing takePicture() INMEDIATAMENTE. Prob IA=${(aiResult.radiographConfidence * 100).toInt()}%"
            )
        } else {
            currentState = CoordinatorCaptureState.RADIOGRAPH_DETECTED
            Log.d("XMEDIC_AI", "COORDINADOR: Radiografía encontrada pero autocaptura está desactivada manualmente.")
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
        Log.d("XMEDIC_AI", "COORDINADOR: Captura de imagen completada exitosamente.")
    }

    fun onCaptureFailed() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.ERROR
        Log.e("XMEDIC_AI", "COORDINADOR: Error en la captura de imagen. Reanudando escáner.")
    }

    fun reset() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.SEARCHING
        Log.d("XMEDIC_AI", "COORDINADOR: Reset de estado ejecutado.")
    }
}
