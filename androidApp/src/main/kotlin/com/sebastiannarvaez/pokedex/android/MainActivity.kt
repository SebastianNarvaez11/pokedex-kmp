package com.sebastiannarvaez.pokedex.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sebastiannarvaez.pokedex.Greeting

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        // El texto viene del modulo compartido: es la prueba de
                        // que Android esta consumiendo Kotlin comun.
                        Saludo(texto = Greeting().greet())
                    }
                }
            }
        }
    }
}

@Composable
private fun Saludo(texto: String, modifier: Modifier = Modifier) {
    Text(text = texto, style = MaterialTheme.typography.titleLarge, modifier = modifier)
}

// La vista previa recibe el texto ya hecho, no el objeto compartido: asi se
// puede pintar en el IDE sin ejecutar nada del modulo shared.
@Preview(showBackground = true)
@Composable
private fun SaludoPreview() {
    MaterialTheme { Saludo(texto = "Hola desde Android 37") }
}
