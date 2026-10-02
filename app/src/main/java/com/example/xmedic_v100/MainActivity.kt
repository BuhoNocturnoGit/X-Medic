package com.example.xmedic_v100

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.xmedic_v100.navigation.XmedicNavHost
import com.example.xmedic_v100.ui.theme.SurfaceWhite
import com.example.xmedic_v100.ui.theme.XmedicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Habilita diseño Edge-to-Edge nativo en Android 14/15
        enableEdgeToEdge()
        
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


