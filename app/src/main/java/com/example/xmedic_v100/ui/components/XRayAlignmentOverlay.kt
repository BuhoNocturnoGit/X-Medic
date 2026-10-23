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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.xmedic_v100.utils.CaptureQualityState
import com.example.xmedic_v100.utils.DetectedDocument
import com.example.xmedic_v100.utils.FrameQualityState

@Composable
fun XRayAlignmentOverlay(
    modifier: Modifier = Modifier,
    detectedDocument: DetectedDocument = DetectedDocument(),
    captureState: CaptureQualityState = CaptureQualityState.SEARCHING_DOCUMENT,
    qualityState: FrameQualityState = FrameQualityState.Insufficient,
    isScanning: Boolean = false,
    scanProgress: Float = 0f
) {
    val bracketColor = when {
        captureState == CaptureQualityState.READY -> Color(0xFF00C2CB)
        qualityState == FrameQualityState.Warning -> Color(0xFFF59E0B)
        qualityState == FrameQualityState.Acceptable -> Color(0xFF00C2CB)
        else -> Color(0xFFEF4444)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val corners = detectedDocument.corners
        if (detectedDocument.isQuadrilateral && corners.size == 4) {
            // Dibuja el marco polígonico dinámico que sigue la geometría real de la radiografía
            val p0 = Offset(corners[0].x * w, corners[0].y * h)
            val p1 = Offset(corners[1].x * w, corners[1].y * h)
            val p2 = Offset(corners[2].x * w, corners[2].y * h)
            val p3 = Offset(corners[3].x * w, corners[3].y * h)

            val quadPath = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            // Fondo semitransparente interior del área detectada
            drawPath(
                path = quadPath,
                color = bracketColor.copy(alpha = 0.12f),
                style = Fill
            )

            // Borde del cuadrilátero dinámico
            drawPath(
                path = quadPath,
                color = bracketColor,
                style = Stroke(width = 3.5f)
            )

            // Esquinas L destacadas en las 4 esquinas reales detectadas
            val cornerLen = 24f
            val cornerStroke = 6f

            // Esquina 0 (Top-Left)
            drawLine(bracketColor, p0, Offset(p0.x + cornerLen, p0.y), strokeWidth = cornerStroke)
            drawLine(bracketColor, p0, Offset(p0.x, p0.y + cornerLen), strokeWidth = cornerStroke)

            // Esquina 1 (Top-Right)
            drawLine(bracketColor, p1, Offset(p1.x - cornerLen, p1.y), strokeWidth = cornerStroke)
            drawLine(bracketColor, p1, Offset(p1.x, p1.y + cornerLen), strokeWidth = cornerStroke)

            // Esquina 2 (Bottom-Right)
            drawLine(bracketColor, p2, Offset(p2.x - cornerLen, p2.y), strokeWidth = cornerStroke)
            drawLine(bracketColor, p2, Offset(p2.x, p2.y - cornerLen), strokeWidth = cornerStroke)

            // Esquina 3 (Bottom-Left)
            drawLine(bracketColor, p3, Offset(p3.x + cornerLen, p3.y), strokeWidth = cornerStroke)
            drawLine(bracketColor, p3, Offset(p3.x, p3.y - cornerLen), strokeWidth = cornerStroke)

            // Línea de escaneo animada sobre la región detectada
            if (isScanning) {
                val scanY = p0.y + ((p2.y - p0.y) * scanProgress)
                drawLine(
                    color = Color(0xFF00C2CB),
                    start = Offset(p0.x, scanY),
                    end = Offset(p1.x, scanY),
                    strokeWidth = 4f
                )
            }
        } else {
            // Marco guía suave de búsqueda cuando aún no se detecta la radiografía
            val boxWidth = w * 0.82f
            val boxHeight = h * 0.72f
            val left = (w - boxWidth) / 2f
            val top = (h - boxHeight) / 2f

            drawRoundRect(
                color = bracketColor.copy(alpha = 0.45f),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(width = 2f)
            )
        }
    }
}
