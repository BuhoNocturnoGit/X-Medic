package com.example.xmedic_v100

import android.graphics.Bitmap
import com.example.xmedic_v100.utils.CaptureQualityEvaluator
import com.example.xmedic_v100.utils.CaptureQualityState
import com.example.xmedic_v100.utils.DetectedDocument
import com.example.xmedic_v100.utils.IXRayDocumentDetector
import com.example.xmedic_v100.utils.QualityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureQualityEvaluatorTest {

    @Test
    fun searchingDocumentWhenDetectorReturnsLowConfidence() {
        val fakeDetector = object : IXRayDocumentDetector {
            override fun detectDocument(bitmap: Bitmap) = DetectedDocument(confidence = 0.1f)
        }
        val evaluator = CaptureQualityEvaluator(detector = fakeDetector)

        val result = evaluator.evaluateFrame(unsafeBitmap())

        assertEquals(CaptureQualityState.SEARCHING_DOCUMENT, result.state)
        assertEquals(QualityLevel.REJECTED, result.level)
        assertFalse(result.isShutterEnabled)
    }

    @Test
    fun outOfFrameWhenDocumentIsCutOff() {
        val fakeDetector = object : IXRayDocumentDetector {
            override fun detectDocument(bitmap: Bitmap) = DetectedDocument(
                confidence = 0.9f,
                isQuadrilateral = true,
                isCutOff = true
            )
        }
        val evaluator = CaptureQualityEvaluator(detector = fakeDetector)

        val result = evaluator.evaluateFrame(unsafeBitmap())

        assertEquals(CaptureQualityState.OUT_OF_FRAME, result.state)
        assertEquals(QualityLevel.REJECTED, result.level)
        assertFalse(result.isShutterEnabled)
    }

    @Test
    fun acceptableQualityEnablesShutterAfterStabilization() {
        val fakeDetector = object : IXRayDocumentDetector {
            override fun detectDocument(bitmap: Bitmap) = DetectedDocument(
                confidence = 0.85f,
                isQuadrilateral = true,
                areaRatio = 0.40f,
                isCutOff = false,
                isTilted = false
            )
        }
        val evaluator = CaptureQualityEvaluator(detector = fakeDetector)

        // Fotograma 1: Verificando
        evaluator.evaluateFrame(unsafeBitmap())
        // Fotograma 2: Verificando
        evaluator.evaluateFrame(unsafeBitmap())
        // Fotograma 3: Estabilizado -> READY
        val res3 = evaluator.evaluateFrame(unsafeBitmap())

        assertEquals(CaptureQualityState.READY, res3.state)
        assertTrue(res3.level == QualityLevel.ACCEPTABLE || res3.level == QualityLevel.EXCELLENT)
        assertTrue(res3.isShutterEnabled)
    }

    private fun unsafeBitmap(): Bitmap {
        val constructor = Bitmap::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance()
    }
}
