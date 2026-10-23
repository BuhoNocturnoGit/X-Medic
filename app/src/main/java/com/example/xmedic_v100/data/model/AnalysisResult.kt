package com.example.xmedic_v100.data.model

import android.net.Uri
import com.example.xmedic_v100.utils.ClassificationFinding

data class AnalysisResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: Uri,
    val modelVersion: String = "Xmedic TFLite v1.0",
    val findings: List<ClassificationFinding> = emptyList(),
    val tokensConsumed: Int = 1,
    val statusMessage: String = "Análisis completado exitosamente",
    val isModelAvailable: Boolean = true
)
