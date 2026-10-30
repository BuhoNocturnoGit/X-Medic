package com.example.xmedic_v100.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
 * Modelo: YOLO11n-cls exportado a TFLite. Entrada: 224x224 RGB con recorte central.
 * Clases: Index 0 = no_radiografia, Index 1 = radiografia.
 */
class RadiographClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var confidenceThreshold = 0.90f // Umbral inicial de autocaptura: 90% (Index 1 = radiografia)

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
            val scaledBitmap = resizeCenterCrop(bitmap, targetSize)

            // Preprocesamiento RGB directo [1, 224, 224, 3] en Float32 (rango 0.0f .. 1.0f)
            val inputBuffer = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(targetSize * targetSize)
            scaledBitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)


            // El modelo espera [1, 3, 224, 224] = NCHW.
            // Se escriben todos los R, después todos los G y todos los B.
            for (channel in 0 until 3) {
                for (pixel in pixels) {
                    val value = when (channel) {
                        0 -> (pixel shr 16) and 0xFF // Rojo
                        1 -> (pixel shr 8) and 0xFF  // Verde
                        else -> pixel and 0xFF       // Azul
                    }

                    inputBuffer.putFloat(value / 255.0f)
                }
            }




            inputBuffer.rewind()

            // Salida [1, 2] para clasificación binaria
            val outputBuffer = Array(1) { FloatArray(2) }
            interp.run(inputBuffer, outputBuffer)

            val rawOutputs = outputBuffer[0]
            val probs = applySoftmaxIfNeeded(rawOutputs)

            val inferenceTime = System.currentTimeMillis() - startTime
            val nonRadProb = probs.getOrElse(0) { 0f }
            val radProb = probs.getOrElse(1) { 0f }

            val isRadiograph = radProb >= confidenceThreshold

            Log.d(
                "XMEDIC_AI",
                "TFLITE INFERENCIA -> Index0(NoRadiografía): ${(nonRadProb * 100).toInt()}% | Index1(Radiografía): ${(radProb * 100).toInt()}% | LogitsBrutos=[${rawOutputs[0]}, ${rawOutputs[1]}] | EsRadiografía: $isRadiograph | Latencia: ${inferenceTime}ms"
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

    /**
     * Redimensiona conservando la proporción de aspecto original y aplica recorte central de 224x224.
     * Replica exactamente el preprocesamiento predeterminado de YOLO11n-cls de Ultralytics.
     */
    private fun resizeCenterCrop(
        source: Bitmap,
        targetSize: Int = 224
    ): Bitmap {
        require(source.width > 0 && source.height > 0)

        val scale = targetSize.toFloat() / minOf(source.width, source.height)

        val newWidth = maxOf(
            targetSize,
            kotlin.math.round(source.width * scale).toInt()
        )

        val newHeight = maxOf(
            targetSize,
            kotlin.math.round(source.height * scale).toInt()
        )

        val resized = Bitmap.createScaledBitmap(
            source,
            newWidth,
            newHeight,
            true
        )

        val left = (newWidth - targetSize) / 2
        val top = (newHeight - targetSize) / 2

        return Bitmap.createBitmap(
            resized,
            left,
            top,
            targetSize,
            targetSize
        )
    }

    /**
     * Prueba explícita para clasificar un archivo de imagen JPG desde almacenamiento/galería
     * utilizando exactamente la misma ruta de preprocesamiento que la cámara.
     */
    fun classifyJpgFromUri(imageUri: Uri): RadiographClassificationResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return RadiographClassificationResult()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e("XMEDIC_AI", "PRUEBA JPG: No se pudo decodificar el bitmap desde Uri $imageUri")
                return RadiographClassificationResult()
            }

            Log.d("XMEDIC_AI", "=== INICIANDO PRUEBA JPG DESDE ALMACENAMIENTO ===")
            Log.d("XMEDIC_AI", "Imagen JPG origen: ${bitmap.width}x${bitmap.height} píxeles")

            val resStandard = classify(bitmap)
            val resImageNet = classifyWithImageNetNorm(bitmap)

            Log.d("XMEDIC_AI", "RESULTADO COMPENSADO ESTÁNDAR (0..1) -> Index1(Radiografía)=${(resStandard.radiographConfidence * 100).toInt()}%")
            Log.d("XMEDIC_AI", "RESULTADO DIAGNÓSTICO IMAGENET -> Index1(Radiografía)=${(resImageNet.radiographConfidence * 100).toInt()}%")
            Log.d("XMEDIC_AI", "=== FIN DE PRUEBA JPG ===")

            resStandard
        } catch (e: Exception) {
            Log.e("XMEDIC_AI", "Error en prueba JPG de almacenamiento", e)
            RadiographClassificationResult()
        }
    }

    private fun classifyWithImageNetNorm(bitmap: Bitmap): RadiographClassificationResult {
        val interp = interpreter ?: return RadiographClassificationResult()
        val targetSize = 224
        val scaledBitmap = resizeCenterCrop(bitmap, targetSize)

        val inputBuffer = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(targetSize * targetSize)
        scaledBitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        for (pixel in pixels) {
            val r = ((pixel shr 16 and 0xFF) / 255.0f - 0.485f) / 0.229f
            val g = ((pixel shr 8 and 0xFF) / 255.0f - 0.456f) / 0.224f
            val b = ((pixel and 0xFF) / 255.0f - 0.406f) / 0.225f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        inputBuffer.rewind()

        val outputBuffer = Array(1) { FloatArray(2) }
        interp.run(inputBuffer, outputBuffer)

        val probs = applySoftmaxIfNeeded(outputBuffer[0])
        val radProb = probs.getOrElse(1) { 0f }
        val nonRadProb = probs.getOrElse(0) { 0f }

        return RadiographClassificationResult(
            isRadiograph = radProb >= confidenceThreshold,
            radiographConfidence = radProb,
            nonRadiographConfidence = nonRadProb,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun applySoftmaxIfNeeded(logits: FloatArray): FloatArray {
        val sum = logits.sum()
        if (kotlin.math.abs(sum - 1.0f) < 0.05f && logits.all { it in 0.0f..1.0f }) {
            return logits
        }
        val maxLogit = logits.maxOrNull() ?: 0f
        val exp = logits.map { kotlin.math.exp((it - maxLogit).toDouble()) }
        val expSum = exp.sum()
        return FloatArray(logits.size) { i -> (exp[i] / expSum).toFloat() }
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
