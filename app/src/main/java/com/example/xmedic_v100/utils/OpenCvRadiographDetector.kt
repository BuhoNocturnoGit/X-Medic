package com.example.xmedic_v100.utils

class OpenCvRadiographDetector(
    private val openCvDetector: IXRayDocumentDetector = OpenCVDocumentDetector()
) : RadiographDetector {

    override suspend fun detect(frame: DetectionFrame): DetectionResult {
        val doc = openCvDetector.detectDocument(frame.bitmap)
        return DetectionResult(
            detectedDocument = doc,
            confidence = doc.confidence,
            detectorSource = DetectorSource.OPENCV_GEOMETRIC,
            timestamp = frame.timestamp,
            isAvailable = true
        )
    }
}
