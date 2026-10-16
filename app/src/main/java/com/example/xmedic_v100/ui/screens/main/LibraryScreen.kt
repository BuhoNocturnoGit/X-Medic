package com.example.xmedic_v100.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xmedic_v100.ui.components.XmedicEmblem
import com.example.xmedic_v100.ui.theme.DarkNavy
import com.example.xmedic_v100.ui.theme.SurfaceWhite
import com.example.xmedic_v100.ui.theme.TextSlate

@Composable
fun LibraryScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = com.example.xmedic_v100.ui.theme.BackgroundPaleMint,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    XmedicEmblem(modifier = Modifier.fillMaxWidth(0.3f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Biblioteca Médica Xmedic",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Aquí encontrarás tus análisis radiográficos, reportes y guías de salud guardadas.",
                fontSize = 14.sp,
                color = TextSlate,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
