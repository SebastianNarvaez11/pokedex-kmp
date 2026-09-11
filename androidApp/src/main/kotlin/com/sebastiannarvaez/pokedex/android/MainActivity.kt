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
import com.sebastiannarvaez.pokedex.feature.auth.EnlaceDeRecuperacion
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

    /**
     * El enlace del correo de recuperacion.
     *
     * Va aparte del destino porque no es una pantalla: trae una sesion y abre
     * un dialogo encima de la app. Mezclarlos obligaria a que el mapa de
     * navegacion tuviera un caso que no navega a ningun sitio.
     */
    private val recuperacionEntrante = mutableStateOf<EnlaceDeRecuperacion?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        recibir(intent?.dataString)

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
                RaizConSesion(
                    recuperacion = recuperacionEntrante.value,
                    alConsumirRecuperacion = { recuperacionEntrante.value = null },
                ) {
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
        recibir(intent.dataString)
    }

    /**
     * Un enlace entrante es una de dos cosas, y el orden importa: el de
     * recuperacion **tambien** encaja en el esquema de la app, asi que hay que
     * descartarlo antes de intentar leerlo como pantalla.
     */
    private fun recibir(url: String?) {
        val enlace = url ?: return
        val recuperacion = EnlaceDeRecuperacion.parse(enlace)
        if (recuperacion != null) {
            recuperacionEntrante.value = recuperacion
        } else {
            destinoEntrante.value = Destination.parse(enlace)
        }
    }
}
