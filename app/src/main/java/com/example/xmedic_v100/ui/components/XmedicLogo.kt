package com.example.xmedic_v100.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xmedic_v100.ui.theme.AccentRed
import com.example.xmedic_v100.ui.theme.DarkNavy
import com.example.xmedic_v100.ui.theme.PrimaryTeal

/**
 * Isotipo representativo de Xmedic: Cruz estilizada en forma de "X" médica con ilustración de pulmones
 */
@Composable
fun XmedicEmblem(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Brazo diagonal principal 1 (Azul oscuro / Teal profundo)
        val path1 = Path().apply {
            moveTo(w * 0.22f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.85f)
        }
        
        // Brazo diagonal 2
        val path2 = Path().apply {
            moveTo(w * 0.78f, h * 0.15f)
            lineTo(w * 0.22f, h * 0.85f)
        }

        // Fondo estilizado de la 'X' con bordes redondeados
        drawRoundRect(
            color = Color(0xFF0F5B78),
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
        )

        // Lóbulo pulmonar izquierdo en color blanco/cian
        val leftLung = Path().apply {
            moveTo(w * 0.48f, h * 0.32f)
            cubicTo(w * 0.35f, h * 0.30f, w * 0.30f, h * 0.48f, w * 0.34f, h * 0.64f)
            cubicTo(w * 0.38f, h * 0.72f, w * 0.46f, h * 0.70f, w * 0.48f, h * 0.60f)
            close()
        }
        drawPath(path = leftLung, color = Color.White, style = Fill)

        // Lóbulo pulmonar derecho
        val rightLung = Path().apply {
            moveTo(w * 0.52f, h * 0.32f)
            cubicTo(w * 0.65f, h * 0.30f, w * 0.70f, h * 0.48f, w * 0.66f, h * 0.64f)
            cubicTo(w * 0.62f, h * 0.72f, w * 0.54f, h * 0.70f, w * 0.52f, h * 0.60f)
            close()
        }
        drawPath(path = rightLung, color = Color.White, style = Fill)

        // Tráquea y bifurcación bronquial en tono cian
        val airway = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            lineTo(w * 0.50f, h * 0.46f)
            moveTo(w * 0.50f, h * 0.46f)
            lineTo(w * 0.42f, h * 0.54f)
            moveTo(w * 0.50f, h * 0.46f)
            lineTo(w * 0.58f, h * 0.54f)
        }
        drawPath(
            path = airway,
            color = PrimaryTeal,
            style = Stroke(width = w * 0.035f)
        )
    }
}

/**
 * Logo completo de Xmedic con tipografía de marca y el icónico punto coral en la 'i'
 */
@Composable
fun XmedicBrandLogo(
    modifier: Modifier = Modifier,
    showText: Boolean = true,
    fontSize: TextUnit = 32.sp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        XmedicEmblem(modifier = Modifier.size(fontSize.value.dp * 2.2f))

        if (showText) {
            Spacer(modifier = Modifier.height(10.dp))
            
            // Texto estilizado 'Xmedic' con el punto coral en la letra 'i'
            val brandText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = DarkNavy, fontWeight = FontWeight.ExtraBold)) {
                    append("Xmed")
                }
                withStyle(style = SpanStyle(color = AccentRed, fontWeight = FontWeight.ExtraBold)) {
                    append("i")
                }
                withStyle(style = SpanStyle(color = DarkNavy, fontWeight = FontWeight.ExtraBold)) {
                    append("c")
                }
            }

            Text(
                text = brandText,
                fontSize = fontSize,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
