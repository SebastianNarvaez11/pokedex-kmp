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
     * Devuelve un token valido, refrescando si esta a punto de caducar.
     *
     * Es el camino normal: se mira el reloj y casi nunca hay que ir a la red.
     */
    suspend fun tokenValido(): String? = withContext(dispatchers.io) {
        val actual = _state.value.session ?: return@withContext null
        if (!actual.caducaAntesDe(ahoraEnSegundos())) actual.accessToken
        else refrescar(actual.refreshToken)?.accessToken
    }

    /**
     * El par que necesita el cliente autenticado.
     *
     * Devuelve los dos porque el plugin de Ktor guarda ambos: el de acceso para
     * la cabecera y el de refresco para su propio reintento.
     */
    suspend fun tokensVigentes(): Pair<String, String>? {
        tokenValido() ?: return null
        val sesion = _state.value.session ?: return null
        return sesion.accessToken to sesion.refreshToken
    }

    /**
     * Refresca aunque el reloj diga que aun vale.
     *
     * Lo llama el cliente cuando el servidor ya ha respondido 401. Ahi no sirve
     * mirar la hora: el servidor ha dicho que no, y las razones sobran —el
     * reloj del movil va adelantado, la sesion se cerro desde otro
     * dispositivo, un administrador la revoco—.
     */
    suspend fun refrescarAhora(): Pair<String, String>? = withContext(dispatchers.io) {
        val actual = _state.value.session ?: return@withContext null
        val nueva = refrescar(actual.refreshToken) ?: return@withContext null
        nueva.accessToken to nueva.refreshToken
    }

    /**
     * El refresco, con candado.
     *
     * El token de refresco de Supabase **es de un solo uso**: si dos peticiones
     * lo gastan a la vez, la segunda recibe un 401 y la sesion se invalida
     * entera. El mutex los serializa, y quien llega segundo se encuentra el
     * trabajo hecho.
     *
     * `gastado` es la clave de esa segunda parte: es el token con el que el que
     * llama venia. Si dentro del candado ya no es el vigente, alguien refresco
     * mientras esperabamos y no hay nada que hacer. Sin esta comprobacion, el
     * mutex serializaria los refrescos pero no los evitaria, que es justo lo
     * que se queria impedir.
     */
    private suspend fun refrescar(gastado: String): Session? = candado.withLock {
        val dentro = _state.value.session ?: return@withLock null
        if (dentro.refreshToken != gastado) return@withLock dentro

        try {
            publicar(api.refrescar(gastado))
        } catch (e: Throwable) {
            // Si el refresco falla, la sesion esta muerta. Dejarla puesta haria
            // que cada peticion fallara con 401 sin explicacion.
            store.borrar()
            _state.value = AuthState(comprobando = false, session = null)
            throw e
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
