package com.sebastiannarvaez.pokedex.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sebastiannarvaez.pokedex.android.navigation.PokedexApp
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebastiannarvaez.pokedex.android.ui.PokedexTheme
import com.sebastiannarvaez.pokedex.feature.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // El tema se lee en la raiz y no dentro de la pantalla de ajustes:
            // si no, cambiarlo solo repintaria esa pantalla.
            val ajustes: SettingsViewModel = koinViewModel()
            val preferencias by ajustes.settings.collectAsStateWithLifecycle()

            PokedexTheme(tema = preferencias.tema) {
                PokedexApp()
            }
        }
    }
}
