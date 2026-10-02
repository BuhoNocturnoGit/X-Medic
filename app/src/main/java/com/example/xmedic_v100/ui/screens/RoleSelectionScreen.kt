package com.example.xmedic_v100.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xmedic_v100.ui.components.InfoBanner
import com.example.xmedic_v100.ui.components.XmedicBackButton
import com.example.xmedic_v100.ui.components.XmedicBrandLogo
import com.example.xmedic_v100.ui.components.XmedicPrimaryButton
import com.example.xmedic_v100.ui.theme.DarkNavy
import com.example.xmedic_v100.ui.theme.PrimaryTeal
import com.example.xmedic_v100.ui.theme.SurfaceWhite
import com.example.xmedic_v100.ui.theme.TextSlate

enum class UserRole(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    PACIENTE(
        title = "Paciente",
        description = "Consultas, seguimiento y bienestar.",
        icon = Icons.Outlined.FavoriteBorder
    ),
    ESTUDIANTE(
        title = "Estudiante",
        description = "Aprendizaje y recursos de salud.",
        icon = Icons.Outlined.School
    ),
    PROFESIONAL(
        title = "Profesional de la salud",
        description = "Atención y gestión de pacientes.",
        icon = Icons.Outlined.MedicalServices
    ),
    EMPRESA(
        title = "Empresa",
        description = "Soluciones para tu organización.",
        icon = Icons.Outlined.Apartment
    )
}

@Composable
fun RoleSelectionScreen(
    onNavigateBack: () -> Unit = {},
    onContinue: (UserRole) -> Unit = {},
    onSkip: () -> Unit = {}
) {
    var selectedRole by remember { mutableStateOf(UserRole.PACIENTE) }
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
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra Superior: Volver, Progreso "Paso 2 de 2" y Logo mini Xmedic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                XmedicBackButton(onClick = onNavigateBack)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Paso 2 de 2",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTeal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryTeal,
                        trackColor = Color(0xFFE2EDF0)
                    )
                }

                // Mini logo Xmedic superior derecho
                XmedicBrandLogo(
                    modifier = Modifier.size(48.dp),
                    showText = true,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Título Principal
            Text(
                text = "¿Cómo usarás Xmedic?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Selecciona el perfil que mejor te representa para adaptar contenidos, accesos y herramientas.",
                fontSize = 14.sp,
                color = TextSlate,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Banner Informativo (Pastilla celeste con ícono 'i')
            InfoBanner(
                text = "Tu selección personalizará la experiencia. Podrás cambiarla más adelante.",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Grid 2x2 de Selección de Perfil
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fila 1: Paciente y Estudiante
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RoleCard(
                        role = UserRole.PACIENTE,
                        isSelected = selectedRole == UserRole.PACIENTE,
                        onSelect = { selectedRole = UserRole.PACIENTE },
                        modifier = Modifier.weight(1f)
                    )
                    RoleCard(
                        role = UserRole.ESTUDIANTE,
                        isSelected = selectedRole == UserRole.ESTUDIANTE,
                        onSelect = { selectedRole = UserRole.ESTUDIANTE },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fila 2: Profesional de la salud y Empresa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RoleCard(
                        role = UserRole.PROFESIONAL,
                        isSelected = selectedRole == UserRole.PROFESIONAL,
                        onSelect = { selectedRole = UserRole.PROFESIONAL },
                        modifier = Modifier.weight(1f)
                    )
                    RoleCard(
                        role = UserRole.EMPRESA,
                        isSelected = selectedRole == UserRole.EMPRESA,
                        onSelect = { selectedRole = UserRole.EMPRESA },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Indicador de perfil seleccionado
            Text(
                text = "Perfil seleccionado: ${selectedRole.title}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryTeal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Esta elección no limita tus funciones y puede modificarse desde el perfil.",
                fontSize = 12.sp,
                color = TextSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Botón Continuar (CTA Principal)
            XmedicPrimaryButton(
                text = "Continuar",
                onClick = { onContinue(selectedRole) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botón Secundario: Configurar después
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Configurar después",
                    color = DarkNavy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Tarjeta de Rol individual con soporte de estado activo y badge de check
 */
@Composable
fun RoleCard(
    role: UserRole,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryTeal else Color(0xFFE2EDF0),
        label = "border_color"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 4.dp else 1.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = PrimaryTeal.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .padding(16.dp)
    ) {
        Column {
            // Icono en contenedor circular
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F6F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = role.icon,
                    contentDescription = role.title,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = role.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = role.description,
                fontSize = 12.sp,
                color = TextSlate,
                lineHeight = 16.sp
            )
        }

        // Badge de verificación en la esquina superior derecha
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
