package com.example.xmedic_v100.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xmedic_v100.ui.components.GoogleSignInButton
import com.example.xmedic_v100.ui.components.SecurityBanner
import com.example.xmedic_v100.ui.components.XmedicBackButton
import com.example.xmedic_v100.ui.components.XmedicEmblem
import com.example.xmedic_v100.ui.components.XmedicPrimaryButton
import com.example.xmedic_v100.ui.components.XmedicTextField
import com.example.xmedic_v100.ui.theme.DarkNavy
import com.example.xmedic_v100.ui.theme.SurfaceWhite
import com.example.xmedic_v100.ui.theme.TextSlate
import com.example.xmedic_v100.ui.viewmodel.LoginUiState
import com.example.xmedic_v100.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onCreateAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isLoading = uiState is LoginUiState.Loading
    val scrollState = rememberScrollState()

    // Manejo de respuesta de inicio de sesión de Google / Firebase
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> {
                onLoginSuccess()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra superior con botón volver
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                XmedicBackButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            // Isotipo centrado con fondo circular suave
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F6F7)),
                contentAlignment = Alignment.Center
            ) {
                XmedicEmblem(modifier = Modifier.size(76.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Encabezados
            Text(
                text = "Bienvenido a Xmedic",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Inicia sesión para acceder a una experiencia de salud\ndiseñada para ti.",
                fontSize = 14.sp,
                color = TextSlate,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Banner para mostrar mensajes de Error o de Confirmación (Restablecimiento de Contraseña)
            when (val state = uiState) {
                is LoginUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is LoginUiState.PasswordResetSent -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = Color(0xFF16A34A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {}
            }

            // Botón Social Google
            GoogleSignInButton(
                onClick = {
                    viewModel.performGoogleSignIn(context)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Divisor 'o usa tu correo'
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
                Text(
                    text = "o usa tu correo",
                    fontSize = 12.sp,
                    color = TextSlate,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input: Correo electrónico
            XmedicTextField(
                value = email,
                onValueChange = { email = it },
                label = "Correo electrónico",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Input: Contraseña
            XmedicTextField(
                value = password,
                onValueChange = { password = it },
                label = "Contraseña",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                keyboardType = KeyboardType.Password,
                modifier = Modifier.fillMaxWidth()
            )

            // ¿Olvidaste tu contraseña?
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkNavy,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onForgotPassword()
                            viewModel.sendPasswordReset(email)
                        }
                    )
                )
            }

            // Botón de Iniciar Sesión (CTA principal)
            XmedicPrimaryButton(
                text = "Iniciar sesión",
                isLoading = isLoading,
                onClick = {
                    viewModel.signInWithEmail(email, password)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Enlace '¿Aún no tienes una cuenta? Crear cuenta'
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Aún no tienes una cuenta? ",
                    fontSize = 13.sp,
                    color = TextSlate
                )
                Text(
                    text = "Crear cuenta",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onCreateAccount()
                            viewModel.createUserWithEmail(email, password)
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Banner de Acceso protegido
            SecurityBanner(
                text = "Acceso protegido y datos tratados de forma segura",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Términos y privacidad
            Text(
                text = "Al continuar, aceptas los Términos y la Política de privacidad.",
                fontSize = 11.sp,
                color = TextSlate.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
