package com.example.xmedic_v100.utils

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Detector de contornos y geometría de radiografías/documentos.
 * Analiza gradientes de bordes, aislando placas radiográficas verdaderas respecto a fondos planos o paredes.
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

        // Submuestra a baja resolución para procesamiento en tiempo real (~5-10ms)
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
        if (contrastRange < 30) {
            framesWithoutDetection++
            if (framesWithoutDetection > 4) {
                smoothedCorners = null
            }
            Log.d("XRayScanner", "Detección rechazada: Escena con contraste uniforme o plano ($contrastRange < 30)")
            return DetectedDocument(confidence = 0.0f, isQuadrilateral = false)
        }

        // Detección de gradientes / bordes locales (Simulación Canny/Sobel)
        val edgeThreshold = (minIntensity + contrastRange * 0.45f).toInt()
        var minX = targetWidth
        var maxX = -1
        var minY = targetHeight
        var maxY = -1
        var activePixels = 0

        for (y in 1 until targetHeight - 1) {
            for (x in 1 until targetWidth - 1) {
                val idx = y * targetWidth + x
                val current = gray[idx]

                // Gradiente horizontal y vertical
                val gx = abs(gray[idx + 1] - gray[idx - 1])
                val gy = abs(gray[idx + targetWidth] - gray[idx - targetWidth])
                val gradient = gx + gy

                if (gradient > 28 && current >= edgeThreshold) {
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

        // Si no hay suficientes píxeles de borde con estructura o el rectángulo abarca el 100% de la pantalla sin bordes reales
        val isFullCanvasBorder = minX <= 1 && maxX >= targetWidth - 2 && minY <= 1 && maxY >= targetHeight - 2

        if (activePixels < 40 || occupation < 0.05f || (isFullCanvasBorder && contrastRange < 80)) {
            framesWithoutDetection++
            if (framesWithoutDetection > 4) {
                smoothedCorners = null
            }
            Log.d("XRayScanner", "Detección rechazada: No hay contorno definido de radiografía (Píxeles borde=$activePixels, Ocupación=${(occupation * 100).toInt()}%)")
            return DetectedDocument(confidence = 0.0f, isQuadrilateral = false)
        }

        // Normalización de coordenadas a rango [0.0f .. 1.0f]
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

        // Suavizado exponencial para evitar saltos o vibraciones del marco
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

        // Verificación si la radiografía toca los bordes del visor
        val isCutOff = rawLeft <= 0.02f || rawRight >= 0.98f || rawTop <= 0.02f || rawBottom >= 0.98f

        // Verificación de inclinación angular
        val deltaX = docWidth.toDouble()
        val deltaY = abs(rawTop.toDouble() - rawTop.toDouble())
        val angle = abs(atan2(deltaY, deltaX) * 180.0 / Math.PI)
        val isTilted = angle > 12.0

        val confidence = if (isCutOff) 0.55f else 0.88f

        Log.d(
            "XRayScanner",
            "Candidato detectado: Área=${(areaRatio * 100).toInt()}%, Confianza=$confidence, Esquinas=TL(${finalCorners[0].x},${finalCorners[0].y}) BR(${finalCorners[2].x},${finalCorners[2].y}), Cortado=$isCutOff, Inclinado=$isTilted"
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
