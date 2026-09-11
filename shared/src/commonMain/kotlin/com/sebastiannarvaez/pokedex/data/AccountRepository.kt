package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.network.SupabaseAccountApi
import kotlinx.coroutines.withContext

/**
 * Lo que se hace **con** la cuenta, ya dentro.
 *
 * Esta separado de `SessionRepository` para romper el ciclo: el cliente
 * autenticado necesita tokens y los tokens los da la sesion, asi que la sesion
 * no puede depender de ese cliente. Aqui si, porque nadie depende de esto.
 */
internal class AccountRepository(
    private val api: SupabaseAccountApi,
    private val session: SessionRepository,
    private val dispatchers: AppDispatchers,
) {

    suspend fun cambiarPassword(nueva: String) = withContext(dispatchers.io) {
        api.cambiarPassword(nueva)
    }

    suspend fun borrarCuenta() = withContext(dispatchers.io) {
        api.borrarCuenta()
        // Se cierra la sesion **despues** de borrar, no antes: si el borrado
        // falla, el usuario sigue dentro y puede reintentar. Al reves quedaria
        // fuera de una cuenta que aun existe.
        session.salir()
    }
}
