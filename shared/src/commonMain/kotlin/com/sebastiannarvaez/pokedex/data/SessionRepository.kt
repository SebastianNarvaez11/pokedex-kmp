package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.network.SupabaseAuthApi
import com.sebastiannarvaez.pokedex.data.network.dto.SessionDto
import com.sebastiannarvaez.pokedex.domain.Session
import com.sebastiannarvaez.pokedex.feature.auth.AuthState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * La sesion, como unica fuente de verdad.
 *
 * Quien quiera saber si hay usuario mira este flujo; nadie pregunta al
 * almacenamiento ni al servidor por su cuenta.
 */
@OptIn(ExperimentalTime::class)
internal class SessionRepository(
    private val api: SupabaseAuthApi,
    private val store: TokenStore,
    private val dispatchers: AppDispatchers,
    private val ahoraEnSegundos: () -> Long = { Clock.System.now().epochSeconds },
) {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * El candado del refresco.
     *
     * El token de refresco de Supabase **es de un solo uso**: si dos peticiones
     * caducadas lo usan a la vez, la segunda recibe un 401 y la sesion se
     * invalida entera. El mutex serializa el refresco, y quien llega segundo se
     * encuentra el token ya renovado.
     */
    private val candado = Mutex()

    suspend fun restaurar() = withContext(dispatchers.io) {
        val guardada = store.leer()
        _state.value = AuthState(comprobando = false, session = guardada)
    }

    suspend fun registrar(email: String, password: String) = withContext(dispatchers.io) {
        publicar(api.registrar(email.trim(), password))
    }

    suspend fun entrar(email: String, password: String) = withContext(dispatchers.io) {
        publicar(api.entrar(email.trim(), password))
    }

    suspend fun salir() = withContext(dispatchers.io) {
        _state.value.session?.let { api.salir(it.accessToken) }
        store.borrar()
        _state.value = AuthState(comprobando = false, session = null)
    }

    /**
     * Devuelve un token valido, refrescando si hace falta.
     *
     * Es lo que llama el interceptor antes de cada peticion protegida.
     */
    suspend fun tokenValido(): String? = withContext(dispatchers.io) {
        val actual = _state.value.session ?: return@withContext null
        if (!actual.caducaAntesDe(ahoraEnSegundos())) return@withContext actual.accessToken

        candado.withLock {
            // Se vuelve a mirar dentro del candado: mientras esperabamos, otro
            // pudo refrescarlo ya. Sin esta segunda comprobacion, el mutex
            // serializa los refrescos pero no los evita.
            val dentro = _state.value.session ?: return@withLock null
            if (!dentro.caducaAntesDe(ahoraEnSegundos())) return@withLock dentro.accessToken

            try {
                publicar(api.refrescar(dentro.refreshToken)).accessToken
            } catch (e: Throwable) {
                // Si el refresco falla, la sesion esta muerta. Dejarla puesta
                // haria que cada peticion fallara con 401 sin explicacion.
                store.borrar()
                _state.value = AuthState(comprobando = false, session = null)
                throw e
            }
        }
    }

    private suspend fun publicar(dto: SessionDto): Session {
        val session = Session(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
            // Se guarda el instante de caducidad, no la duracion: `expires_in`
            // solo es cierto en el momento en que llega.
            expiresAtEpochSeconds = ahoraEnSegundos() + dto.expiresIn,
            userId = dto.user?.id.orEmpty(),
            email = dto.user?.email,
        )
        store.guardar(session)
        _state.value = AuthState(comprobando = false, session = session)
        return session
    }
}
