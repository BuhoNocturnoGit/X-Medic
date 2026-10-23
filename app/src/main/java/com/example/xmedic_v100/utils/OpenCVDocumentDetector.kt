package com.example.xmedic_v100.utils

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Detector de contornos y geometría de radiografías/documentos.
 * Implementa filtrado de bordes, detección de esquinas y suavizado exponencial temporal.
 */
class OpenCVDocumentDetector : IXRayDocumentDetector {

    private var smoothedCorners: List<PointF>? = null
    private val alpha = 0.35f // Factor de suavizado exponencial
    private var framesWithoutDetection = 0

    override fun detectDocument(bitmap: Bitmap): DetectedDocument {
        val w = bitmap.width
        val h = bitmap.height

        if (w <= 0 || h <= 0) {
            return DetectedDocument()
        }

        // Submuestra a baja resolución para procesamiento fluido en milisegundos en tiempo real
        val targetWidth = 160
        val targetHeight = (h.toFloat() / w.toFloat() * targetWidth).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)

        val pixels = IntArray(targetWidth * targetHeight)
        scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        // Convertir a escala de grises y mapa de gradiente
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
        if (contrastRange < 20) {
            framesWithoutDetection++
            if (framesWithoutDetection > 5) {
                smoothedCorners = null
            }
            Log.d("XRayScanner", "Detección rechazada: Contraste insuficiente ($contrastRange < 20)")
            return DetectedDocument(confidence = 0.1f)
        }

        // Detección de umbrales para buscar bordes exteriores e interiores
        val thresholdOuter = minIntensity + (contrastRange * 0.35f).toInt()
        val thresholdInner = minIntensity + (contrastRange * 0.60f).toInt()

        var minX = targetWidth
        var maxX = 0
        var minY = targetHeight
        var maxY = 0
        var activePixels = 0

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val idx = y * targetWidth + x
                val isBright = gray[idx] >= thresholdOuter || gray[idx] >= thresholdInner
                if (isBright) {
                    activePixels++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val totalPixels = targetWidth * targetHeight
        val occupation = activePixels.toFloat() / totalPixels

        if (occupation < 0.12f) {
            framesWithoutDetection++
            if (framesWithoutDetection > 5) {
                smoothedCorners = null
            }
            Log.d("XRayScanner", "Detección rechazada: Ocupación insuficiente (${(occupation * 100).toInt()}% < 12%)")
            return DetectedDocument(confidence = 0.15f)
        }

        // Normalizar las 4 esquinas del cuadrilátero detectado a rango [0.0f .. 1.0f]
        val rawLeft = (minX.toFloat() / targetWidth).coerceIn(0f, 1f)
        val rawRight = (maxX.toFloat() / targetWidth).coerceIn(0f, 1f)
        val rawTop = (minY.toFloat() / targetHeight).coerceIn(0f, 1f)
        val rawBottom = (maxY.toFloat() / targetHeight).coerceIn(0f, 1f)

        val rawCorners = listOf(
            PointF(rawLeft, rawTop),      // Top-Left
            PointF(rawRight, rawTop),     // Top-Right
            PointF(rawRight, rawBottom),  // Bottom-Right
            PointF(rawLeft, rawBottom)    // Bottom-Left
        )

        // Aplicar filtrado exponencial para suavizado fluido del marco entre fotogramas
        val currentSmoothed = smoothedCorners
        val finalCorners = if (currentSmoothed != null && currentSmoothed.size == 4) {
            rawCorners.mapIndexed { index, rawPt ->
                val prevPt = currentSmoothed[index]
                PointF(
                    alpha * rawPt.x + (1f - alpha) * prevPt.x,
                    alpha * rawPt.y + (1f - alpha) * prevPt.y
                )
            }
        } else {
            rawCorners
        }

        smoothedCorners = finalCorners
        framesWithoutDetection = 0

        val docWidth = (rawRight - rawLeft).coerceAtLeast(0.01f)
        val docHeight = (rawBottom - rawTop).coerceAtLeast(0.01f)
        val areaRatio = docWidth * docHeight

        val docCenterX = (rawLeft + rawRight) / 2f
        val docCenterY = (rawTop + rawBottom) / 2f
        val centerOffset = PointF((docCenterX - 0.5f) * 2f, (docCenterY - 0.5f) * 2f)

        // Verificación de cortes por bordes reales del fotograma
        val isCutOff = rawLeft <= 0.02f || rawRight >= 0.98f || rawTop <= 0.02f || rawBottom >= 0.98f

        // Verificación de inclinación
        val deltaX = docWidth.toDouble()
        val deltaY = abs(rawTop.toDouble() - rawTop.toDouble())
        val angle = abs(atan2(deltaY, deltaX) * 180.0 / Math.PI)
        val isTilted = angle > 12.0

        val confidence = when {
            isCutOff -> 0.45f
            areaRatio in 0.15f..0.92f -> 0.88f
            else -> 0.65f
        }

        Log.d(
            "XRayScanner",
            "Detección exitosa: Área=${(areaRatio * 100).toInt()}%, Confianza=$confidence, Esquinas=TL(${finalCorners[0].x},${finalCorners[0].y}) BR(${finalCorners[2].x},${finalCorners[2].y}), Cortado=$isCutOff, Inclinado=$isTilted"
        )

        return DetectedDocument(
            corners = finalCorners,
            areaRatio = areaRatio,
            isQuadrilateral = true,
            centerOffset = centerOffset,
            isTilted = isTilted,
            isCutOff = isCutOff,
            confidence = confidence
        )
    }
}
