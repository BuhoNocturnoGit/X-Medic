package com.example.xmedic_v100.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xmedic_v100.data.repository.UserRepository
import com.example.xmedic_v100.ui.components.XmedicEmblem
import com.example.xmedic_v100.ui.theme.DarkNavy
import com.example.xmedic_v100.ui.theme.PrimaryTeal
import com.example.xmedic_v100.ui.theme.SurfaceWhite
import com.example.xmedic_v100.ui.theme.TextSlate

@Composable
fun HomeScreen(
    onOpenStore: () -> Unit = {}
) {
    val context = LocalContext.current
    val userRepository = UserRepository(context)
    val userName = userRepository.getUserDisplayName()
    val tokenCount = userRepository.getTokenCount()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    XmedicEmblem(modifier = Modifier.size(38.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Buenos días,", fontSize = 12.sp, color = TextSlate)
                        Text("Hola, $userName", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de Tokens (Clickable para abrir tienda)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { onOpenStore() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$tokenCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Campana de notificaciones con punto rojo
                    Box {
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notificaciones", tint = DarkNavy)
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E))
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Banner Gana Tokens
            Surface(
                color = Color(0xFF0F5B78),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gana Tokens", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Completa tareas y canjea créditos por evaluaciones gratuitas.",
                            color = Color(0xFFE2EDF0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onOpenStore,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DarkNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ver tareas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Box(modifier = Modifier.size(60.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E))
                                .align(Alignment.TopStart),
                            contentAlignment = Alignment.Center
                        ) { Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal)
                                .border(2.dp, Color(0xFF0F5B78), CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) { Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Noticias para ti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Noticias para ti", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                Text("Ver todas", fontSize = 13.sp, color = PrimaryTeal, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { })
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 1 Destacada
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    // Ilustración de pulmones
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F6F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        XmedicEmblem(modifier = Modifier.size(54.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                            Text(
                                "SALUD RESPIRATORIA",
                                color = PrimaryTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("5 hábitos para cuidar tus pulmones", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Consejos simples para proteger tu salud respiratoria cada día.", fontSize = 12.sp, color = TextSlate, lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("4 min de lectura", fontSize = 11.sp, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lista de otras noticias
            NewsItemCard(icon = Icons.Default.Star, title = "¿Cuándo es recomendable una radiografía?", tag = "Prevención · 3 min")
            Spacer(modifier = Modifier.height(10.dp))
            NewsItemCard(icon = Icons.Default.NightlightRound, title = "Descanso y salud: señales que debes observar", tag = "Bienestar · 5 min")
            Spacer(modifier = Modifier.height(10.dp))
            NewsItemCard(icon = Icons.Default.CheckCircle, title = "Chequeos preventivos según tu edad", tag = "Cuidado · 4 min")

            Spacer(modifier = Modifier.height(80.dp))
        }

        // FAB de cámara flotante
        Box(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.BottomEnd)
                .size(56.dp)
                .shadow(6.dp, CircleShape, spotColor = PrimaryTeal)
                .clip(CircleShape)
                .background(PrimaryTeal)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Escanear", tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
fun NewsItemCard(icon: ImageVector, title: String, tag: String) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2EDF0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F6F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                Spacer(modifier = Modifier.height(2.dp))
                Text(tag, fontSize = 11.sp, color = TextSlate)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSlate, modifier = Modifier.size(18.dp))
        }
    }
}
