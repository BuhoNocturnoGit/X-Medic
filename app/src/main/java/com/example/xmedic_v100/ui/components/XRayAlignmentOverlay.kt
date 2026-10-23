package com.example.xmedic_v100.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.xmedic_v100.utils.FrameQualityState

@Composable
fun XRayAlignmentOverlay(
    modifier: Modifier = Modifier,
    qualityState: FrameQualityState = FrameQualityState.Acceptable,
    isScanning: Boolean = false,
    scanProgress: Float = 0f
) {
    val bracketColor = when (qualityState) {
        FrameQualityState.Acceptable -> Color(0xFF00C2CB)
        FrameQualityState.Warning -> Color(0xFFF59E0B)
        FrameQualityState.Insufficient -> Color(0xFFEF4444)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val boxWidth = w * 0.82f
        val boxHeight = h * 0.75f
        val left = (w - boxWidth) / 2f
        val top = (h - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Marco de guía exterior con bordes redondeados
        drawRoundRect(
            color = bracketColor.copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 3.5f)
        )

        // Esquinas de alineación destacadas
        val bracketLength = 36f
        val bracketStroke = 7f

        // Esquina superior izquierda
        drawLine(bracketColor, Offset(left, top), Offset(left + bracketLength, top), strokeWidth = bracketStroke)
        drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLength), strokeWidth = bracketStroke)

        // Esquina superior derecha
        drawLine(bracketColor, Offset(right, top), Offset(right - bracketLength, top), strokeWidth = bracketStroke)
        drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLength), strokeWidth = bracketStroke)

        // Esquina inferior izquierda
        drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLength, bottom), strokeWidth = bracketStroke)
        drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - bracketLength), strokeWidth = bracketStroke)

        // Esquina inferior derecha
        drawLine(bracketColor, Offset(right, bottom), Offset(right - bracketLength, bottom), strokeWidth = bracketStroke)
        drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - bracketLength), strokeWidth = bracketStroke)

        // Guías anatómicas de tórax (costillas y columna sutiles)
        val centerX = w / 2f
        val centerY = h / 2f

        // Línea central de la columna vertebral
        drawLine(
            color = Color(0x4000C2CB),
            start = Offset(centerX, top + 30f),
            end = Offset(centerX, bottom - 30f),
            strokeWidth = 2f
        )

        // Lóbulos pulmonares de guía
        val leftRib = Path().apply {
            moveTo(centerX - 10f, top + boxHeight * 0.25f)
            cubicTo(left + 20f, top + boxHeight * 0.20f, left + 20f, top + boxHeight * 0.70f, centerX - 10f, top + boxHeight * 0.80f)
        }
        drawPath(leftRib, color = Color(0x3000C2CB), style = Stroke(width = 2f))

        val rightRib = Path().apply {
            moveTo(centerX + 10f, top + boxHeight * 0.25f)
            cubicTo(right - 20f, top + boxHeight * 0.20f, right - 20f, top + boxHeight * 0.70f, centerX + 10f, top + boxHeight * 0.80f)
        }
        drawPath(rightRib, color = Color(0x3000C2CB), style = Stroke(width = 2f))

        // Diana/Target central
        drawCircle(
            color = bracketColor.copy(alpha = 0.5f),
            radius = 18f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Línea de escaneo animada durante el análisis
        if (isScanning) {
            val scanY = top + (boxHeight * scanProgress)
            drawLine(
                color = Color(0xFF00C2CB),
                start = Offset(left, scanY),
                end = Offset(right, scanY),
                strokeWidth = 4f
            )
        }
    }
}
