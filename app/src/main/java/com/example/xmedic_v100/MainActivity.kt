
package com.example.xmedic_v100

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

import com.example.xmedic_v100.navigation.XmedicNavHost
import com.example.xmedic_v100.ui.theme.SurfaceWhite
import com.example.xmedic_v100.ui.theme.XmedicTheme

// NUEVO: SDK de Google AdMob
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NUEVO: Inicializar Google AdMob
        Thread {
            MobileAds.initialize(this) {}
        }.start()

        // Mantener diseño Edge-to-Edge
        enableEdgeToEdge()

        // Mantener interfaz y navegación de XMedic
        setContent {
            XmedicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceWhite
                ) {
                    XmedicNavHost()
                }
            }
        }
    }
}


