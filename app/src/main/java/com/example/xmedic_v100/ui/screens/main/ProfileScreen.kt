package com.example.xmedic_v100.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xmedic_v100.data.repository.UserRepository
import com.example.xmedic_v100.ui.theme.DarkNavy
import com.example.xmedic_v100.ui.theme.PrimaryTeal
import com.example.xmedic_v100.ui.theme.SurfaceWhite
import com.example.xmedic_v100.ui.theme.TextSlate

@Composable
fun ProfileScreen(
    onOpenStore: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val userRepository = UserRepository(context)
    val userName = userRepository.getUserDisplayName()
    val userRole = userRepository.getUserRole()
    val tokenCount = userRepository.getTokenCount()
    val userInitials = userRepository.getUserInitials()

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
            // Header Bar: Título "Perfil" + Ícono Ajustes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Perfil",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ajustes",
                        tint = DarkNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Usuario (Iniciales, Nombre, Rol Badge, Editar)
            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2EDF0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar Círculo con Iniciales
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F6F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userInitials,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Rol Badge
                            Surface(
                                color = Color(0xFFE8F6F7),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = userRole.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryTeal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Botón Editar
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar perfil",
                            tint = TextSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Saldo Disponible Tokens (Dark Navy / Cyan)
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
                        Text(
                            text = "SALDO DISPONIBLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00C2CB)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$tokenCount",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tokens",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Úsalos en evaluaciones y beneficios.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onOpenStore,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = DarkNavy
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Ganar más", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Plan Actual (Freemium)
            Surface(
                color = Color(0xFFEBF8FA),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PLAN ACTUAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Freemium",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Funciones esenciales",
                                fontSize = 12.sp,
                                color = TextSlate
                            )
                        }
                    }

                    Button(
                        onClick = onOpenStore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PrimaryTeal
                        ),
                        border = BorderStroke(1.dp, PrimaryTeal),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Ver Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección Configuración
            Text(
                text = "Configuración",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2EDF0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    SettingRowItem(icon = Icons.Outlined.Security, title = "Cuenta y seguridad")
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingRowItem(icon = Icons.Outlined.Notifications, title = "Notificaciones")
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingRowItem(icon = Icons.Outlined.Lock, title = "Privacidad y datos")
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingRowItem(icon = Icons.Outlined.Payment, title = "Planes y facturación")
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingRowItem(icon = Icons.AutoMirrored.Outlined.TextSnippet, title = "Términos y condiciones")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enlace Rojo Cerrar sesión
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSignOut
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cerrar sesión",
                    color = Color(0xFFEF4444),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SettingRowItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F6F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkNavy
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSlate,
            modifier = Modifier.size(20.dp)
        )
    }
}
