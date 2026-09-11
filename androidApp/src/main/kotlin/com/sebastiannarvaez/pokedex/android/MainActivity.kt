package com.sebastiannarvaez.pokedex.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebastiannarvaez.pokedex.android.auth.RaizConSesion
import com.sebastiannarvaez.pokedex.android.navigation.PokedexApp
import com.sebastiannarvaez.pokedex.android.ui.PokedexTheme
import com.sebastiannarvaez.pokedex.feature.settings.SettingsViewModel
import com.sebastiannarvaez.pokedex.navigation.Destination
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    /**
     * El destino que trajo el enlace, si lo hubo.
     *
     * Es un estado observable y no un valor calculado una vez, porque la app
     * puede recibir un enlace **ya abierta**: entonces no hay `onCreate`, hay
     * `onNewIntent`, y la pantalla tiene que reaccionar.
     */
    private val destinoEntrante = mutableStateOf<Destination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        destinoEntrante.value = intent?.dataString?.let(Destination::parse)

        setContent {
            // El tema se lee en la raiz y no dentro de la pantalla de ajustes:
            // si no, cambiarlo solo repintaria esa pantalla.
            val ajustes: SettingsViewModel = koinViewModel()
            val preferencias by ajustes.settings.collectAsStateWithLifecycle()

            PokedexTheme(tema = preferencias.tema) {
                // El enlace entrante se guarda en el estado de la actividad, no
                // aqui dentro. Por eso un enlace que llega sin sesion no se
                // pierde: espera a que la app se componga, que es despues de
                // entrar.
                RaizConSesion {
                    PokedexApp(
                        destinoEntrante = destinoEntrante.value,
                        alConsumirDestino = { destinoEntrante.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Sin esto, un enlace recibido con la app abierta no hace nada: el
        // sistema la trae al frente y ya. Es el fallo mas comun de los enlaces
        // profundos, porque solo se reproduce con la app en segundo plano.
        destinoEntrante.value = intent.dataString?.let(Destination::parse)
    }
}
