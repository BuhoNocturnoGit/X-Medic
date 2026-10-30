package com.example.xmedic_v100.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class RadiographClassificationResult(
    val isRadiograph: Boolean = false,
    val radiographConfidence: Float = 0f,
    val nonRadiographConfidence: Float = 0f,
    val inferenceTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Clasificador binario de radiografías en tiempo real con TensorFlow Lite (best.tflite).
 * Modelo: YOLO11n-cls exportado a TFLite. Entrada: 224x224 RGB.
 * Clases: Index 0 = no_radiografia, Index 1 = radiografia.
 */
class RadiographClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var confidenceThreshold = 0.65f // Umbral de confianza inicial calibrado para cámara real (Index 1 = radiografia)

    init {
        try {
            val modelBuffer = loadModelFile(context, "best.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true

            val inputTensor = interpreter?.getInputTensor(0)
            val outputTensor = interpreter?.getOutputTensor(0)

            Log.d(
                "XMEDIC_AI",
                "MODELO TFLITE CARGADO: 'best.tflite' | InputShape=${inputTensor?.shape()?.contentToString()} (${inputTensor?.dataType()}) | OutputShape=${outputTensor?.shape()?.contentToString()} (${outputTensor?.dataType()})"
            )
        } catch (e: Exception) {
            Log.e("XMEDIC_AI", "ERROR CRÍTICO AL CARGAR MODELO TFLITE 'best.tflite'", e)
            isInitialized = false
        }
    }

    fun setConfidenceThreshold(threshold: Float) {
        this.confidenceThreshold = threshold.coerceIn(0.1f, 0.99f)
    }

    fun classify(bitmap: Bitmap): RadiographClassificationResult {
        val interp = interpreter
        if (!isInitialized || interp == null) {
            Log.w("XMEDIC_AI", "Clasificación omitida: El modelo TFLite no está inicializado.")
            return RadiographClassificationResult(timestamp = System.currentTimeMillis())
        }

        val startTime = System.currentTimeMillis()
        return try {
            val targetSize = 224
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, false)

            // Preprocesamiento RGB directo [1, 224, 224, 3] en Float32 (rango 0.0f .. 1.0f)
            val inputBuffer = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(targetSize * targetSize)
            scaledBitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                inputBuffer.putFloat(r / 255.0f)
                inputBuffer.putFloat(g / 255.0f)
                inputBuffer.putFloat(b / 255.0f)
            }

            // Salida [1, 2] para clasificación binaria
            val outputBuffer = Array(1) { FloatArray(2) }
            interp.run(inputBuffer, outputBuffer)

            val inferenceTime = System.currentTimeMillis() - startTime
            val nonRadProb = outputBuffer[0].getOrElse(0) { 0f }
            val radProb = outputBuffer[0].getOrElse(1) { 0f }

            val isRadiograph = radProb >= confidenceThreshold

            Log.d(
                "XMEDIC_AI",
                "TFLITE INFERENCIA -> Index0(NoRadiografía): ${(nonRadProb * 100).toInt()}% | Index1(Radiografía): ${(radProb * 100).toInt()}% | Umbral: ${(confidenceThreshold * 100).toInt()}% | EsRadiografía: $isRadiograph | Latencia: ${inferenceTime}ms"
            )

            RadiographClassificationResult(
                isRadiograph = isRadiograph,
                radiographConfidence = radProb,
                nonRadiographConfidence = nonRadProb,
                inferenceTimeMs = inferenceTime,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("XMEDIC_AI", "Error durante la ejecución de la inferencia TFLite", e)
            RadiographClassificationResult(timestamp = System.currentTimeMillis())
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            isInitialized = false
            Log.d("XMEDIC_AI", "Intérprete TFLite liberado.")
        } catch (e: Exception) {
            Log.e("XMEDIC_AI", "Error al cerrar el intérprete TFLite", e)
        }
    }
}
