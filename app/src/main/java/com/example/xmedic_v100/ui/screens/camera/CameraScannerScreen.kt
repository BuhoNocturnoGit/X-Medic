package com.example.xmedic_v100.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.xmedic_v100.data.repository.UserRepository
import com.example.xmedic_v100.ui.components.CameraView
import com.example.xmedic_v100.ui.components.XRayAlignmentOverlay
import com.example.xmedic_v100.ui.theme.ClinicalBgCanvas
import com.example.xmedic_v100.ui.theme.ClinicalBorderStrong
import com.example.xmedic_v100.ui.theme.ClinicalBorderSubtle
import com.example.xmedic_v100.ui.theme.ClinicalPrimary
import com.example.xmedic_v100.ui.theme.ClinicalSurfaceBase
import com.example.xmedic_v100.ui.theme.ClinicalSurfaceElevated
import com.example.xmedic_v100.ui.theme.ClinicalTextHighContrast
import com.example.xmedic_v100.ui.theme.ClinicalTextMuted
import com.example.xmedic_v100.utils.FrameQualityState
import com.example.xmedic_v100.utils.ImageQualityAnalysis

@Composable
fun CameraScannerScreen(
    onBackClick: () -> Unit = {},
    onImageConfirmed: (Uri) -> Unit = {},
    onTokensClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    val tokens = userRepository.getTokenCount()

    var isFlashOn by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var triggerHardwareCapture by remember { mutableStateOf(false) }

    var qualityAnalysis by remember { mutableStateOf(ImageQualityAnalysis()) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmationStep by remember { mutableStateOf(false) }

    // Solicitar permiso de cámara
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Android Photo Picker para Galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedImageUri = uri
            showConfirmationStep = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalBgCanvas)
    ) {
        if (showConfirmationStep && capturedImageUri != null) {
            // Paso 9: Confirmación de Imagen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Confirmación de Imagen",
                    color = ClinicalTextHighContrast,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, ClinicalBorderStrong, RoundedCornerShape(20.dp))
                ) {
                    AsyncImage(
                        model = capturedImageUri,
                        contentDescription = "Vista previa de la radiografía",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "Asegúrate de que la radiografía esté clara y legible antes de realizar el análisis.",
                    color = ClinicalTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            capturedImageUri = null
                            showConfirmationStep = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClinicalSurfaceElevated,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Volver a tomar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            capturedImageUri?.let { onImageConfirmed(it) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClinicalPrimary,
                            contentColor = Color(0xFF0B131E)
                        ),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Usar esta imagen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Pantalla Principal de Escáner de Cámara
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("btn_camera_back")
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ClinicalSurfaceElevated)
                            .border(1.dp, ClinicalBorderStrong, RoundedCornerShape(12.dp))
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al inicio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Captura de radiografía",
                        color = ClinicalTextHighContrast,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .testTag("btn_camera_flash_toggle")
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isFlashOn) ClinicalPrimary.copy(alpha = 0.2f) else ClinicalSurfaceElevated)
                            .border(
                                1.dp,
                                if (isFlashOn) ClinicalPrimary else ClinicalBorderStrong,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isFlashOn = !isFlashOn },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Outlined.FlashOn,
                            contentDescription = "Alternar flash",
                            tint = if (isFlashOn) ClinicalPrimary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Alignment Guide Subtitle Pill
                Box(
                    modifier = Modifier
                        .testTag("pill_alignment_guide")
                        .clip(RoundedCornerShape(20.dp))
                        .background(ClinicalSurfaceBase)
                        .border(1.dp, ClinicalBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = qualityAnalysis.guidanceMessage,
                        color = when (qualityAnalysis.state) {
                            FrameQualityState.Acceptable -> ClinicalPrimary
                            FrameQualityState.Warning -> Color(0xFFF59E0B)
                            FrameQualityState.Insufficient -> Color(0xFFEF4444)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Viewfinder Container with real camera + XRayAlignmentOverlay
                Box(
                    modifier = Modifier
                        .testTag("viewfinder_box")
                        .fillMaxWidth()
                        .weight(1f)
                        .aspectRatio(0.78f, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Transparent)
                        .border(1.dp, ClinicalBorderStrong, RoundedCornerShape(24.dp))
                ) {
                    if (hasCameraPermission) {
                        CameraView(
                            modifier = Modifier.fillMaxSize(),
                            isFlashOn = isFlashOn,
                            triggerCapture = triggerHardwareCapture,
                            onQualityAnalyzed = { analysis ->
                                qualityAnalysis = analysis
                            },
                            onImageCaptured = { uri ->
                                triggerHardwareCapture = false
                                capturedImageUri = uri
                                showConfirmationStep = true
                            },
                            onCaptureHandled = {
                                triggerHardwareCapture = false
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ClinicalSurfaceBase),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Permiso de cámara necesario para continuar.",
                                color = ClinicalTextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    XRayAlignmentOverlay(
                        modifier = Modifier.fillMaxSize(),
                        qualityState = qualityAnalysis.state,
                        isScanning = isScanning,
                        scanProgress = scanProgress
                    )

                    // Watermark "● SÓCRATES AI"
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC0D141D))
                            .border(1.dp, ClinicalPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ClinicalPrimary)
                            )
                            Text(
                                text = "SÓCRATES AI",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x77070E17)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { scanProgress },
                                    color = ClinicalPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(52.dp)
                                )
                                Text(
                                    text = "Procesando radiografía...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Evita reflejos y mantén la imagen completamente visible.",
                    color = ClinicalTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Galería
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("btn_camera_gallery")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ClinicalSurfaceElevated)
                                .border(1.dp, ClinicalBorderStrong, RoundedCornerShape(16.dp))
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Abrir galería",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Galería",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Botón Shutter
                    val shutterScale by animateFloatAsState(
                        targetValue = if (isScanning) 0.92f else 1f,
                        label = "shutterScale"
                    )

                    Box(
                        modifier = Modifier
                            .testTag("btn_camera_shutter")
                            .size(78.dp)
                            .scale(shutterScale)
                            .clip(CircleShape)
                            .border(3.5.dp, Color.White, CircleShape)
                            .clickable(enabled = !isScanning) {
                                triggerHardwareCapture = true
                            }
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ClinicalPrimary)
                            )
                        }
                    }

                    // Flash Toggle Bottom
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("btn_camera_flash_bottom")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isFlashOn) ClinicalPrimary.copy(alpha = 0.2f) else ClinicalSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isFlashOn) ClinicalPrimary else ClinicalBorderStrong,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { isFlashOn = !isFlashOn },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Outlined.FlashOn,
                                contentDescription = "Flash",
                                tint = if (isFlashOn) ClinicalPrimary else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Flash",
                            color = if (isFlashOn) ClinicalPrimary else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Token Button
                Text(
                    text = "$tokens Tokens disponibles",
                    color = ClinicalPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .testTag("txt_tokens_disponibles")
                        .clickable(onClick = onTokensClick)
                        .padding(bottom = 6.dp)
                )
            }
        }
    }
}
