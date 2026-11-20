package com.example.practica11

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

    // Inicialización de la actividad principal
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de la interfaz mediante Jetpack Compose
        setContent {
            ReproductorScreen()
        }
    }
}