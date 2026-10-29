package com.example.retrofit.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.retrofit.di.Dependencias
import com.example.retrofit.ui.theme.MiMarketTheme
import com.example.retrofit.view.navegacion.MiMarketNavHost
import com.example.retrofit.view.navegacion.Rutas

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val inicio = if (Dependencias.repositorio.haySesion) Rutas.CATALOGO else Rutas.LOGIN
        setContent {
            MiMarketTheme {
                MiMarketNavHost(inicio = inicio, alReiniciar = ::reiniciar)
            }
        }
    }

    private fun reiniciar() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
