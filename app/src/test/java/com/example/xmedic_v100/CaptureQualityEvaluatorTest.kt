package com.example.xmedic_v100

import android.graphics.Bitmap
import com.example.xmedic_v100.utils.CaptureQualityEvaluator
import com.example.xmedic_v100.utils.CaptureQualityState
import com.example.xmedic_v100.utils.DetectedDocument
import com.example.xmedic_v100.utils.IXRayDocumentDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertFalse(result.isShutterEnabled)
    }

    private fun unsafeBitmap(): Bitmap {
        val constructor = Bitmap::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance()
    }
}
