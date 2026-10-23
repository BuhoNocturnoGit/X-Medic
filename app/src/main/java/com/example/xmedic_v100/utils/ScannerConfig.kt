package com.example.xmedic_v100.utils

/**
 * Configuración centralizada de parámetros y umbrales para el escáner inteligente de radiografías.
 */
object ScannerConfig {
    // Ponderaciones de Puntuación de Calidad (Suman 1.0)
    const val WEIGHT_GEOMETRY = 0.40f
    const val WEIGHT_ILLUMINATION = 0.20f
    const val WEIGHT_SHARPNESS = 0.25f
    const val WEIGHT_STABILITY = 0.15f

    // Umbrales de Clasificación de Calidad (0 .. 100)
    const val SCORE_EXCELLENT_THRESHOLD = 85f
    const val SCORE_ACCEPTABLE_THRESHOLD = 60f

    // Umbrales Mínimos Obligatorios
    const val MIN_GEOMETRY_SCORE = 45f
    const val MIN_ILLUMINATION_SCORE = 30f
    const val MIN_SHARPNESS_SCORE = 35f
    const val MIN_STABILITY_SCORE = 35f

    // Tolerancias Geométricas
    const val MAX_TILT_DEGREES = 15.0
    const val MIN_AREA_RATIO = 0.15f
    const val MAX_AREA_RATIO = 0.90f
    const val MAX_CENTER_OFFSET = 0.38f
    const val FRAME_SAFETY_MARGIN = 0.015f

    // Estabilización Temporal
    const val REQUIRED_STABLE_FRAMES = 2 // ~0.8s - 1.0s de estabilización
}
