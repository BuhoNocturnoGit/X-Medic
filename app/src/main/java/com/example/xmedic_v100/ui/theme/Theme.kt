package com.example.xmedic_v100.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Paleta de Colores Extraída del Diseño Original
val PrimaryTeal = Color(0xFF0098A6)
val PrimaryTealDark = Color(0xFF007E8A)
val PrimaryTealLight = Color(0xFFE1F5F7)
val DarkNavy = Color(0xFF0C243B)
val TextSlate = Color(0xFF60778C)
val AccentRed = Color(0xFFEB5757)
val BackgroundPaleMint = Color(0xFFEAF7F8)
val SurfaceWhite = Color(0xFFFFFFFF)
val BorderLight = Color(0xFFE2EDF0)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = PrimaryTealLight,
    onPrimaryContainer = DarkNavy,
    secondary = DarkNavy,
    onSecondary = Color.White,
    background = SurfaceWhite,
    onBackground = DarkNavy,
    surface = SurfaceWhite,
    onSurface = DarkNavy,
    outline = BorderLight
)

val XmedicShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun XmedicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = XmedicShapes,
        content = content
    )
}
