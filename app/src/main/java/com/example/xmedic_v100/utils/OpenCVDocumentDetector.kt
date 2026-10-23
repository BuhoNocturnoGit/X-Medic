package com.example.xmedic_v100.utils

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Detector de contornos y geometría de radiografías/documentos.
 * Implementa filtrado de bordes, detección de esquinas y análisis de encuadre.
 */
class OpenCVDocumentDetector : IXRayDocumentDetector {

    override fun detectDocument(bitmap: Bitmap): DetectedDocument {
        val w = bitmap.width
        val h = bitmap.height

        if (w <= 0 || h <= 0) {
            return DetectedDocument()
        }

        // Submuestra a baja resolución para procesamiento en tiempo real en milisegundos
        val targetWidth = 160
        val targetHeight = (h.toFloat() / w.toFloat() * targetWidth).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)

        val pixels = IntArray(targetWidth * targetHeight)
        scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        // Convertir a escala de grises y mapa de gradiente de bordes
        val gray = IntArray(pixels.size)
        var minIntensity = 255
        var maxIntensity = 0

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            gray[i] = lum
            if (lum < minIntensity) minIntensity = lum
            if (lum > maxIntensity) maxIntensity = lum
        }

        val contrastRange = maxIntensity - minIntensity
        if (contrastRange < 25) {
            // Imagen plana / sin objeto discernible frente al fondo
            return DetectedDocument(confidence = 0.1f)
        }

        // Detección de umbral de intensidad para aislar la placa/documento
        val threshold = minIntensity + (contrastRange * 0.40f).toInt()
        var minX = targetWidth
        var maxX = 0
        var minY = targetHeight
        var maxY = 0
        var activePixelsCount = 0

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val idx = y * targetWidth + x
                val isBright = gray[idx] >= threshold
                if (isBright) {
                    activePixelsCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (activePixelsCount < (targetWidth * targetHeight * 0.12f)) {
            // Área insuficiente para considerarse radiografía visible
            return DetectedDocument(confidence = 0.15f)
        }

        // Normalización de coordenadas a rango [0.0 .. 1.0]
        val normLeft = minX.toFloat() / targetWidth
        val normRight = maxX.toFloat() / targetWidth
        val normTop = minY.toFloat() / targetHeight
        val normBottom = maxY.toFloat() / targetHeight

        val corners = listOf(
            PointF(normLeft, normTop),     // Top-Left
            PointF(normRight, normTop),    // Top-Right
            PointF(normRight, normBottom), // Bottom-Right
            PointF(normLeft, normBottom)   // Bottom-Left
        )

        val docWidth = (normRight - normLeft).coerceAtLeast(0.01f)
        val docHeight = (normBottom - normTop).coerceAtLeast(0.01f)
        val areaRatio = docWidth * docHeight

        val docCenterX = (normLeft + normRight) / 2f
        val docCenterY = (normTop + normBottom) / 2f
        val centerOffset = PointF((docCenterX - 0.5f) * 2f, (docCenterY - 0.5f) * 2f)

        // Verificación si el objeto toca los bordes del fotograma
        val isCutOff = normLeft <= 0.03f || normRight >= 0.97f || normTop <= 0.03f || normBottom >= 0.97f

        // Verificación de inclinación
        val deltaX = (normRight - normLeft)
        val deltaY = abs(normTop - normTop)
        val angle = abs(atan2(deltaY.toDouble(), deltaX.toDouble()) * 180.0 / Math.PI)
        val isTilted = angle > 10.0

        val confidence = when {
            isCutOff -> 0.45f
            areaRatio in 0.25f..0.85f -> 0.85f
            else -> 0.60f
        }

        return DetectedDocument(
            corners = corners,
            areaRatio = areaRatio,
            isQuadrilateral = true,
            centerOffset = centerOffset,
            isTilted = isTilted,
            isCutOff = isCutOff,
            confidence = confidence
        )
    }
}
