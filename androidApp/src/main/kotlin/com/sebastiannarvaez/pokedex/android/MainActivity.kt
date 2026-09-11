package com.sebastiannarvaez.pokedex.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sebastiannarvaez.pokedex.android.navigation.PokedexApp
import com.sebastiannarvaez.pokedex.android.ui.PokedexTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PokedexTheme {
                PokedexApp()
            }
        }
    }
}
