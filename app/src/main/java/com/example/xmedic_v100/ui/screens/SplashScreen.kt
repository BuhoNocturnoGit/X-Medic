package com.example.xmedic_v100.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xmedic_v100.ui.components.XmedicBrandLogo
import com.example.xmedic_v100.ui.theme.BackgroundPaleMint
import com.example.xmedic_v100.ui.theme.DarkNavy
import com.example.xmedic_v100.ui.theme.PrimaryTeal
import com.example.xmedic_v100.ui.theme.TextSlate
import com.example.xmedic_v100.ui.viewmodel.SplashDestination
import com.example.xmedic_v100.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToRoleSelection: () -> Unit = {},
    onTimeout: () -> Unit = {}
) {
    val context = LocalContext.current
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkSessionAndNavigate(context)
    }

    // Manejo de la redirección según el estado de la sesión validado por SplashViewModel
    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Login -> {
                onNavigateToLogin()
                onTimeout()
            }
            SplashDestination.RoleSelection -> {
                onNavigateToRoleSelection()
                onTimeout()
            }
            SplashDestination.Home -> {
                onNavigateToHome()
                onTimeout()
            }
            SplashDestination.Idle -> {
                // Esperando validación de sesión y tiempo de retardo mínimo
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPaleMint)
    ) {
        // Círculos decorativos de fondo sutiles
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Anillo superior izquierdo
            drawCircle(
                color = Color(0x1F0098A6),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.15f, size.height * 0.12f),
                style = Stroke(width = 1.5f)
            )
            // Punto de acento coral superior derecho
            drawCircle(
                color = Color(0xFFF87171),
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.14f)
            )
            // Gran arco decorativo inferior derecho
            drawCircle(
                color = Color(0x330098A6),
                radius = size.width * 0.45f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.88f),
                style = Stroke(width = 1.5f)
            )
            // Pequeñas burbujas decorativas
            drawCircle(
                color = Color(0x4D0098A6),
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.19f, size.height * 0.76f)
            )
            drawCircle(
                color = Color(0x800098A6),
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.785f)
            )
        }

        // Contenido Principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Bloque Central: Logo y Textos
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo de Marca Xmedic
                XmedicBrandLogo(
                    modifier = Modifier.size(170.dp),
                    showText = true,
                    fontSize = 38.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Tu salud, conectada contigo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Preparando una experiencia personalizada y\nsegura.",
                    fontSize = 15.sp,
                    color = TextSlate,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Indicador de Carga Animado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ThreeDotsLoader()
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Cargando...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "Salud y tecnología en un solo lugar",
                    fontSize = 12.sp,
                    color = TextSlate.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Animación fluida de tres puntos (pulsing dots)
 */
@Composable
fun ThreeDotsLoader() {
    val dots = listOf(
        remember { Animatable(0.35f) },
        remember { Animatable(0.35f) },
        remember { Animatable(0.35f) }
    )

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 200L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { anim ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal.copy(alpha = anim.value))
            )
        }
    }
}
