package com.example.xmedic_v100.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

enum class FrameQualityState {
    Acceptable,
    Warning,
    Insufficient
}

data class ImageQualityAnalysis(
    val state: FrameQualityState = FrameQualityState.Insufficient,
    val guidanceMessage: String = "Coloca la radiografía dentro del marco",
    val isStableAndReady: Boolean = false,
    val brightness: Float = 0f,
    val sharpness: Float = 0f,
    val hasReflections: Boolean = false
)

object ImageProcessor {

    private var lastGuidanceMessage: String = "Coloca la radiografía dentro del marco"
    private var stableFrameCount: Int = 0

    /**
     * Analiza un Bitmap para evaluar brillo, nitidez, reflejos y encuadre.
     */
    fun analyzeBitmapQuality(bitmap: Bitmap): ImageQualityAnalysis {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) {
            return ImageQualityAnalysis()
        }

        // Submuestra pequeña para análisis rápido de luminancia y brillo
        val sampleSize = 64
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)

        var totalLuminance = 0L
        var overExposedCount = 0
        var underExposedCount = 0

        val pixels = IntArray(sampleSize * sampleSize)
        scaledBitmap.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = (0.299f * r + 0.587f * g + 0.114f * b).toInt()

            totalLuminance += luminance
            if (luminance > 240) overExposedCount++
            if (luminance < 15) underExposedCount++
        }

        val avgBrightness = totalLuminance.toFloat() / (sampleSize * sampleSize)
        val overExposedRatio = overExposedCount.toFloat() / (sampleSize * sampleSize)
        val hasReflections = overExposedRatio > 0.12f

        // Cálculo de varianza para estimar la nitidez
        var varianceSum = 0.0
        for (i in 0 until pixels.size - 1) {
            val p1 = pixels[i] and 0xFF
            val p2 = pixels[i + 1] and 0xFF
            val diff = (p1 - p2).toDouble()
            varianceSum += diff * diff
        }
        val sharpness = (varianceSum / pixels.size).toFloat()
        val isBlurry = sharpness < 15f

        // Determinar mensaje y estado de calidad
        val newMessage: String
        val state: FrameQualityState

        when {
            avgBrightness < 35f -> {
                newMessage = "Hay demasiada oscuridad"
                state = FrameQualityState.Insufficient
            }
            avgBrightness > 220f -> {
                newMessage = "Hay demasiada iluminación"
                state = FrameQualityState.Insufficient
            }
            hasReflections -> {
                newMessage = "Se detectaron reflejos"
                state = FrameQualityState.Warning
            }
            isBlurry -> {
                newMessage = "La imagen está borrosa. Mantén el dispositivo estable"
                state = FrameQualityState.Warning
            }
            else -> {
                newMessage = "Radiografía bien encuadrada. Lista para capturar"
                state = FrameQualityState.Acceptable
            }
        }

        // Estabilización temporal para evitar parpadeo de mensajes
        if (newMessage == lastGuidanceMessage) {
            stableFrameCount++
        } else {
            stableFrameCount = 0
            lastGuidanceMessage = newMessage
        }

        val isReady = state == FrameQualityState.Acceptable && stableFrameCount >= 3

        return ImageQualityAnalysis(
            state = state,
            guidanceMessage = lastGuidanceMessage,
            isStableAndReady = isReady,
            brightness = avgBrightness,
            sharpness = sharpness,
            hasReflections = hasReflections
        )
    }

    /**
     * Procesa la imagen capturada: corrige orientación EXIF y guarda en archivo temporal de caché.
     */
    fun processAndCacheCapturedImage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = false }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return null
            inputStream.close()

            // Corregir orientación EXIF
            val rotatedBitmap = correctExifOrientation(context, sourceUri, bitmap)

            // Guardar en archivo temporal de caché
            val cacheFile = File(context.cacheAreaDir(), "xray_processed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(cacheFile)
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
            outputStream.flush()
            outputStream.close()

            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri
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
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun Context.cacheAreaDir(): File {
        val dir = File(cacheDir, "xray_scans")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
