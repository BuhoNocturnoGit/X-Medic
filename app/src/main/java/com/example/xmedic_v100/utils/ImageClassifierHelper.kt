package com.example.xmedic_v100.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class ClassificationFinding(
    val label: String,
    val confidence: Float
)

sealed interface InferenceResult {
    data class Success(
        val findings: List<ClassificationFinding>,
        val modelVersion: String = "Xmedic TFLite v1.0"
    ) : InferenceResult

    data class ModelMissing(
        val message: String = "El modelo de IA (xmedic_model.tflite) no se encuentra en assets. Por favor agrega el modelo entrenado."
    ) : InferenceResult

    data class Error(
        val message: String
    ) : InferenceResult
}

class ImageClassifierHelper(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val modelFileName = "xmedic_model.tflite"

    fun isModelAvailable(): Boolean {
        return try {
            val inputStream = context.assets.open(modelFileName)
            inputStream.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun runInferenceOnImage(imageUri: Uri): InferenceResult {
        if (!isModelAvailable()) {
            return InferenceResult.ModelMissing(
                "El modelo real de IA (xmedic_model.tflite) no se encuentra en la carpeta assets del proyecto. Agrega el modelo entrenado para obtener análisis clínicos reales."
            )
        }

        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return InferenceResult.Error("No se pudo leer la imagen seleccionada")

            val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: return InferenceResult.Error("No se pudo decodificar el bitmap de la imagen")
            inputStream.close()

            if (interpreter == null) {
                val modelBuffer = loadModelFile() ?: return InferenceResult.ModelMissing()
                interpreter = Interpreter(modelBuffer)
            }

            // Preprocesamiento de tensor de entrada (ejemplo 224x224x3 Float)
            val inputSize = 224
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(inputSize * inputSize)
            scaledBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

            for (pixel in pixels) {
                val r = (pixel shr 16 and 0xFF) / 255.0f
                val g = (pixel shr 8 and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }

            // Matriz de salida para 4 clases de clasificación
            val output = Array(1) { FloatArray(4) }
            interpreter?.run(byteBuffer, output)

            val labels = listOf("Normal", "Infiltrados Pulmonares", "Consolidación", "Derrame Pleural")
            val findings = labels.mapIndexed { index, label ->
                ClassificationFinding(label, output[0].getOrElse(index) { 0f })
            }

            InferenceResult.Success(findings)
        } catch (e: Exception) {
            InferenceResult.Error("Error durante la inferencia TFLite: ${e.localizedMessage}")
        }
    }

    private fun loadModelFile(): ByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd(modelFileName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (_: Exception) {}
    }
}
