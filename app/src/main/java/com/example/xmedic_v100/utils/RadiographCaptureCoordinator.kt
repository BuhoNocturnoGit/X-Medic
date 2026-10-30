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
    @Synchronized
    fun evaluate(
        aiResult: RadiographClassificationResult,
        openCvDoc: DetectedDocument,
        quality: ImageQualityAnalysis,
        isAutoCaptureEnabled: Boolean
    ): CoordinatorDecision {
        val now = System.currentTimeMillis()

        // Corrección 3: Evitar disparos duplicados si la fotografía ya fue capturada previamente
        if (currentState == CoordinatorCaptureState.CAPTURED) {
            return CoordinatorDecision(
                state = CoordinatorCaptureState.CAPTURED,
                guidanceMessage = "Fotografía capturada",
                shouldTriggerCapture = false,
                isAiPositive = aiResult.isRadiograph,
                aiConfidence = aiResult.radiographConfidence,
                timestamp = now
            )
        }

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
                quality.brightness in 1f..14f -> "Hay poca iluminación. Mejora la luz ambiental"
                quality.brightness > 240f -> "Demasiada iluminación. Reduce la luz directa"
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

        // Corrección 2: Comprobación flexible de encuadre combinando evidencia geométrica, iluminación y textura
        val hasUsefulContour = openCvDoc.confidence >= 0.35f && openCvDoc.areaRatio in 0.12f..0.96f
        val exposureAcceptable = quality.brightness in 15f..240f
        val textureAcceptable = quality.sharpness >= 12f

        val framingAcceptable = exposureAcceptable && (hasUsefulContour || textureAcceptable)

        if (!framingAcceptable) {
            val warningMsg = when {
                quality.brightness < 15f -> "Hay poca iluminación. Mejora la luz ambiental"
                quality.brightness > 240f -> "Demasiada iluminación"
                quality.hasReflections -> "Evita los reflejos sobre la imagen"
                else -> "Ajusta el encuadre"
            }
            Log.d("XMEDIC_AI", "COORDINADOR: IA Positiva pero encuadre inaceptable (Brillo=${quality.brightness}, Nitidez=${quality.sharpness}). Message=$warningMsg")
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

    @Synchronized
    fun onCaptureCompleted() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.CAPTURED
        Log.d("XMEDIC_AI", "COORDINADOR: Captura de imagen completada exitosamente.")
    }

    @Synchronized
    fun onCaptureFailed() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.ERROR
        Log.e("XMEDIC_AI", "COORDINADOR: Error en la captura de imagen. Reanudando escáner.")
    }

    @Synchronized
    fun reset() {
        isCaptureInProgress = false
        currentState = CoordinatorCaptureState.SEARCHING
        Log.d("XMEDIC_AI", "COORDINADOR: Reset de estado ejecutado.")
    }
}
