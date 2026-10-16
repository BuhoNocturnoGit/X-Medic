package com.example.xmedic_v100.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenStoreScreen(
    tokenCount: Int = 120,
    onNavigateBack: () -> Unit = {}
) {
    val bgDark = Color(0xFF0B131E)
    val cyan = Color(0xFF00C2CB)
    val cardBg = Color(0xFF132030)
    val cardBorder = Color(0xFF1E324B)

    Scaffold(
        containerColor = bgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tokens y Suscripción",
                        color = Color(0xFFF1F5F9),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, cardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFFE2E8F0)
                        )
                    }
                },
                actions = {
                    Surface(
                        color = cardBg,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$tokenCount", color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Balance Summary
            item {
                Surface(
                    color = Color(0xFF16273B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SALDO ACTIVO", color = cyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$tokenCount", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tokens", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Text("Válidos para análisis radiográficos y reportes", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.size(48.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E))
                                    .align(Alignment.TopStart),
                                contentAlignment = Alignment.Center
                            ) { Text("T", color = Color.White, fontWeight = FontWeight.Bold) }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(cyan)
                                    .border(2.dp, cardBg, CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) { Text("T", color = Color(0xFF08202A), fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }

            // Token Packages
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Paquetes de Tokens", color = Color(0xFFF1F5F9), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Sin fecha de caducidad", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TokenPackageCard(title = "Básico", tokens = "10", price = "$4.99", highlighted = false)
                        TokenPackageCard(title = "Popular", tokens = "50 + 10", price = "$19.99", highlighted = true, tag = "Mejor Valor", extraTokens = "+10 Gratis")
                        TokenPackageCard(title = "Profesional", tokens = "150 + 40", price = "$49.99", highlighted = false, extraTokens = "+40 Gratis", extraTokenColor = Color(0xFF34D399), extraTokenBg = Color(0xFF34D399).copy(alpha = 0.1f))
                    }
                }
            }

            // Subscription Plan
            item {
                Column {
                    Text("Suscripción Ilimitada", color = Color(0xFFF1F5F9), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    Surface(
                        color = Color(0xFF102033),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, cyan.copy(alpha = 0.4f)),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Surface(
                                color = cyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, cyan.copy(alpha = 0.3f))
                            ) {
                                Text("RECOMENDADO PARA ESPECIALISTAS", color = cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Plan Médico Pro", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("Acceso sin límites a diagnósticos con IA", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$29", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                    Text("/mes", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = cardBorder.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(16.dp))
                            PlanFeature(text = "Evaluaciones ilimitadas")
                            Spacer(modifier = Modifier.height(10.dp))
                            PlanFeature(text = "Reportes clínicos con firma digital")
                            Spacer(modifier = Modifier.height(10.dp))
                            PlanFeature(text = "Prioridad máxima en cola de IA")
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cyan, contentColor = Color(0xFF08202A)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Prueba gratuita de 7 días", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cancela en cualquier momento sin penalizaciones", color = Color(0xFF94A3B8), fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Free Tokens
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Gana tokens gratis", color = Color(0xFFF1F5F9), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Completar tareas", color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FreeTokenTaskCard(title = "Completar perfil médico", subtitle = "Verifica especialidad médica", reward = "+5 T")
                        FreeTokenTaskCard(title = "Invitar colega médico", subtitle = "Cuando complete su primera lectura", reward = "+15 T")
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun TokenPackageCard(
    title: String,
    tokens: String,
    price: String,
    highlighted: Boolean,
    tag: String? = null,
    extraTokens: String? = null,
    extraTokenColor: Color = Color(0xFF00C2CB),
    extraTokenBg: Color = Color(0xFF00C2CB).copy(alpha = 0.15f)
) {
    val cardBg = if (highlighted) Color(0xFF16273C) else Color(0xFF132030)
    val cardBorder = if (highlighted) Color(0xFF00C2CB) else Color(0xFF1E324B)
    
    Box {
        Surface(
            color = cardBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(if (highlighted) 2.dp else 1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (tag != null) 8.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A293E))
                            .border(1.dp, Color(0xFF263D5A), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("T", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (extraTokens != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = extraTokenBg, shape = RoundedCornerShape(4.dp)) {
                                    Text(extraTokens, color = extraTokenColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("$tokens Tokens (total)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(price, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("USD", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(bottom = 1.dp))
                        }
                    }
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highlighted) Color(0xFF00C2CB) else Color(0xFF1A293E),
                        contentColor = if (highlighted) Color(0xFF0B131E) else Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Comprar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        if (tag != null) {
            Surface(
                color = Color(0xFF00C2CB),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(tag.uppercase(), color = Color(0xFF082329), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
fun PlanFeature(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF00C2CB).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color(0xFF00C2CB),
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color(0xFFE2E8F0), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FreeTokenTaskCard(title: String, subtitle: String, reward: String) {
    Surface(
        color = Color(0xFF132030),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E324B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Cyan.copy(alpha = 0.1f))
                        .border(1.dp, Color.Cyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFF00C2CB),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, color = Color(0xFFF1F5F9), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
            Surface(
                color = Color(0xFF00C2CB).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF00C2CB).copy(alpha = 0.2f))
            ) {
                Text(reward, color = Color(0xFF00C2CB), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}
