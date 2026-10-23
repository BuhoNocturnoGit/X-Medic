package com.example.xmedic_v100.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

enum class FrameQualityState {
    Acceptable,
    Warning,
    Insufficient
}

data class ImageQualityAnalysis(
    val state: FrameQualityState = FrameQualityState.Insufficient,
    val guidanceMessage: String = "Buscando radiografía...",
    val isStableAndReady: Boolean = false,
    val brightness: Float = 0f,
    val sharpness: Float = 0f,
    val hasReflections: Boolean = false
)

object ImageProcessor {

    private var lastGuidanceMessage: String = "Buscando radiografía..."
    private var stableFrameCount: Int = 0

    /**
     * Analiza un Bitmap para evaluar brillo, nitidez y reflejos especulares.
     */
    fun analyzeBitmapQuality(bitmap: Bitmap): ImageQualityAnalysis {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) {
            return ImageQualityAnalysis()
        }

        val sampleSize = 64
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)

        var totalLuminance = 0L
        var overExposedCount = 0

        val pixels = IntArray(sampleSize * sampleSize)
        scaledBitmap.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = (0.299f * r + 0.587f * g + 0.114f * b).toInt()

            totalLuminance += luminance
            if (luminance > 240) overExposedCount++
        }

        val avgBrightness = totalLuminance.toFloat() / (sampleSize * sampleSize)
        val overExposedRatio = overExposedCount.toFloat() / (sampleSize * sampleSize)
        val hasReflections = overExposedRatio > 0.12f

        // Varianza para estimar la nitidez
        var varianceSum = 0.0
        for (i in 0 until pixels.size - 1) {
            val p1 = pixels[i] and 0xFF
            val p2 = pixels[i + 1] and 0xFF
            val diff = (p1 - p2).toDouble()
            varianceSum += diff * diff
        }
        val sharpness = (varianceSum / pixels.size).toFloat()

        return ImageQualityAnalysis(
            state = if (sharpness >= 12f && !hasReflections) FrameQualityState.Acceptable else FrameQualityState.Warning,
            guidanceMessage = lastGuidanceMessage,
            isStableAndReady = true,
            brightness = avgBrightness,
            sharpness = sharpness,
            hasReflections = hasReflections
        )
    }

    /**
     * Procesa la imagen capturada de alta resolución: corrige orientación EXIF y aplica recorte inteligente si se proporcionan las esquinas.
     */
    fun processAndCacheCapturedImage(
        context: Context,
        sourceUri: Uri,
        corners: List<PointF>? = null
    ): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = false }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return null
            inputStream.close()

            // 1. Corregir orientación EXIF
            val rotatedBitmap = correctExifOrientation(context, sourceUri, bitmap)

            // 2. Aplicar recorte inteligente basado en las 4 esquinas reales detectadas
            val croppedBitmap = if (corners != null && corners.size == 4) {
                cropBitmapByCorners(rotatedBitmap, corners)
            } else {
                rotatedBitmap
            }

            // 3. Guardar en archivo temporal de caché
            val cacheFile = File(context.cacheAreaDir(), "xray_processed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(cacheFile)
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
            outputStream.flush()
            outputStream.close()

            Log.d("XRayScanner", "Imagen procesada y guardada en caché: ${cacheFile.absolutePath}")
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.e("XRayScanner", "Error al procesar la imagen capturada", e)
            sourceUri
        }
    }

    private fun cropBitmapByCorners(bitmap: Bitmap, corners: List<PointF>): Bitmap {
        return try {
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()

            val left = (corners.minOf { it.x } * bw).coerceIn(0f, bw - 1f).toInt()
            val top = (corners.minOf { it.y } * bh).coerceIn(0f, bh - 1f).toInt()
            val right = (corners.maxOf { it.x } * bw).coerceIn(1f, bw).toInt()
            val bottom = (corners.maxOf { it.y } * bh).coerceIn(1f, bh).toInt()

            val cropW = (right - left).coerceAtLeast(10)
            val cropH = (bottom - top).coerceAtLeast(10)

            Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun correctExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun Context.cacheAreaDir(): File {
        val dir = File(cacheDir, "xray_scans")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
