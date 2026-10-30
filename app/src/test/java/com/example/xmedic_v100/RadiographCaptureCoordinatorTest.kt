package com.example.xmedic_v100

import com.example.xmedic_v100.utils.CoordinatorCaptureState
import com.example.xmedic_v100.utils.DetectedDocument
import com.example.xmedic_v100.utils.ImageQualityAnalysis
import com.example.xmedic_v100.utils.RadiographCaptureCoordinator
import com.example.xmedic_v100.utils.RadiographClassificationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadiographCaptureCoordinatorTest {

    @Test
    fun aiPositiveTriggersImmediateCaptureWithoutDelay() {
        val coordinator = RadiographCaptureCoordinator()
        val aiResult = RadiographClassificationResult(
            isRadiograph = true,
            radiographConfidence = 0.94f
        )
        val openCvDoc = DetectedDocument(confidence = 0.50f)
        val quality = ImageQualityAnalysis(brightness = 100f, sharpness = 20f)

        val decision = coordinator.evaluate(
            aiResult = aiResult,
            openCvDoc = openCvDoc,
            quality = quality,
            isAutoCaptureEnabled = true
        )

        assertEquals("Capturando...", decision.guidanceMessage)
        assertTrue(decision.shouldTriggerCapture)
        assertEquals(CoordinatorCaptureState.CAPTURING, decision.state)
    }

    @Test
    fun aiPositiveWithAutoCaptureDisabledShowsRadiographFound() {
        val coordinator = RadiographCaptureCoordinator()
        val aiResult = RadiographClassificationResult(
            isRadiograph = true,
            radiographConfidence = 0.94f
        )
        val openCvDoc = DetectedDocument(confidence = 0.50f)
        val quality = ImageQualityAnalysis(brightness = 100f, sharpness = 20f)

        val decision = coordinator.evaluate(
            aiResult = aiResult,
            openCvDoc = openCvDoc,
            quality = quality,
            isAutoCaptureEnabled = false
        )

        assertEquals("Radiografía encontrada", decision.guidanceMessage)
        assertFalse(decision.shouldTriggerCapture)
        assertEquals(CoordinatorCaptureState.RADIOGRAPH_DETECTED, decision.state)
    }

    @Test
    fun aiNegativeDoesNotTriggerCapture() {
        val coordinator = RadiographCaptureCoordinator()
        val aiResult = RadiographClassificationResult(
            isRadiograph = false,
            radiographConfidence = 0.12f
        )
        val openCvDoc = DetectedDocument(confidence = 0.90f)
        val quality = ImageQualityAnalysis(brightness = 100f, sharpness = 20f)

        val decision = coordinator.evaluate(
            aiResult = aiResult,
            openCvDoc = openCvDoc,
            quality = quality,
            isAutoCaptureEnabled = true
        )

        assertEquals("Buscando radiografía...", decision.guidanceMessage)
        assertFalse(decision.shouldTriggerCapture)
        assertEquals(CoordinatorCaptureState.SEARCHING, decision.state)
    }

    @Test
    fun preventsDuplicateCapturesWhileCapturingIsInProgress() {
        val coordinator = RadiographCaptureCoordinator()
        val aiResult = RadiographClassificationResult(
            isRadiograph = true,
            radiographConfidence = 0.92f
        )
        val openCvDoc = DetectedDocument()
        val quality = ImageQualityAnalysis(brightness = 100f, sharpness = 20f)

        // Primer fotograma: activa el disparo
        val dec1 = coordinator.evaluate(aiResult, openCvDoc, quality, isAutoCaptureEnabled = true)
        assertTrue(dec1.shouldTriggerCapture)

        // Segundo fotograma subsiguiente: bloquea disparos duplicados
        val dec2 = coordinator.evaluate(aiResult, openCvDoc, quality, isAutoCaptureEnabled = true)
        assertFalse(dec2.shouldTriggerCapture)
        assertEquals(CoordinatorCaptureState.CAPTURING, dec2.state)
    }
}
