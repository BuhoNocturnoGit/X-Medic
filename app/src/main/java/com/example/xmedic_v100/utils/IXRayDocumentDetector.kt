package com.example.xmedic_v100.utils

import android.graphics.Bitmap
import android.graphics.PointF

/**
 * Representa un cuadrilátero/documento detectado en la escena.
 * Las coordenadas de las esquinas están normalizadas entre 0.0f y 1.0f.
 */
data class DetectedDocument(
    val corners: List<PointF> = emptyList(), // Top-Left, Top-Right, Bottom-Right, Bottom-Left
    val areaRatio: Float = 0f, // Área del documento relativa al área del fotograma (0.0..1.0)
    val isQuadrilateral: Boolean = false,
    val centerOffset: PointF = PointF(0f, 0f), // Desplazamiento desde el centro (-1.0..1.0)
    val isTilted: Boolean = false,
    val isCutOff: Boolean = false,
    val confidence: Float = 0f
)

/**
 * Interfaz para la detección geométrica de radiografías/documentos.
 * Permite intercambiar el motor de procesamiento (OpenCV, algoritmos nativos o IA visión en el futuro).
 */
interface IXRayDocumentDetector {
    fun detectDocument(bitmap: Bitmap): DetectedDocument
}
