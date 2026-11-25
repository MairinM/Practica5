package com.example.practica5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.practica5.ui.screens.MythologyScreen
import com.example.practica5.ui.theme.Practica5Theme // Ajusta si tu Theme.kt tiene otro nombre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Practica5Theme {
                MythologyScreen()
            }
        }
    }
}