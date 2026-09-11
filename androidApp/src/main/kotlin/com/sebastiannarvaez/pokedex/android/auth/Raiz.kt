package com.sebastiannarvaez.pokedex.android.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebastiannarvaez.pokedex.core.AppConfig
import org.koin.compose.koinInject
import com.sebastiannarvaez.pokedex.feature.auth.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * El guardian de la app.
 *
 * Aqui esta la decision entera: **hay tres estados y solo uno ensena la app**.
 * Ponerlo en la raiz y no dentro de la navegacion tiene una consecuencia que se
 * agradece: no existe ninguna ruta que alguien pueda alcanzar sin sesion,
 * porque sin sesion la pila de pantallas **ni siquiera se compone**. No hay
 * nada que proteger una por una, ni una lista de rutas publicas que se olvide
 * de actualizar al anadir la siguiente.
 *
 * El precio es que al cerrar sesion se pierde la pila. En una app asi es lo
 * correcto: nadie quiere volver y encontrarse la ficha que estaba mirando otro.
 */
@Composable
fun RaizConSesion(
    contenido: @Composable () -> Unit,
) {
    val config: AppConfig = koinInject()

    // Sin Supabase configurado no hay puerta que guardar. Es lo que permite
    // clonar el repositorio y ejecutar la app sin crear ninguna cuenta.
    if (!config.haySupabase) {
        contenido()
        return
    }

    val viewModel: AuthViewModel = koinViewModel()
    val estado by viewModel.state.collectAsStateWithLifecycle()

    when {
        estado.comprobando -> ComprobandoSesion()
        estado.haySesion -> contenido()
        else -> AuthScreen(viewModel = viewModel)
    }
}
