package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.domain.Session
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Donde se guardan los tokens.
 *
 * Es una interfaz porque cada plataforma tiene su sitio, y porque un test no
 * deberia escribir en el llavero del sistema para comprobar una regla de
 * negocio.
 */
interface TokenStore {
    suspend fun leer(): Session?
    suspend fun guardar(session: Session)
    suspend fun borrar()
}

/** Para tests y para arrancar sin nada montado. */
class InMemoryTokenStore : TokenStore {
    private var session: Session? = null
    override suspend fun leer(): Session? = session
    override suspend fun guardar(session: Session) { this.session = session }
    override suspend fun borrar() { session = null }
}

/**
 * El puente entre la sesion y el sitio seguro de cada plataforma.
 *
 * Vive en el codigo comun, asi que el formato de lo guardado es **el mismo en
 * las dos**. Si cada plataforma serializara a su manera, un cambio en `Session`
 * habria que recordarlo dos veces.
 */
internal class SecureStorageTokenStore(
    private val storage: SecureStorage,
    private val dispatchers: AppDispatchers,
) : TokenStore {

    override suspend fun leer(): Session? = withContext(dispatchers.io) {
        val guardado = storage.leer(CLAVE) ?: return@withContext null
        // Si lo guardado es de una version anterior y ya no encaja, no se
        // revienta: se trata como «no hay sesion» y el usuario vuelve a entrar.
        runCatching { json.decodeFromString<SessionGuardada>(guardado).aSession() }.getOrNull()
    }

    override suspend fun guardar(session: Session) = withContext(dispatchers.io) {
        storage.guardar(CLAVE, json.encodeToString(SessionGuardada.de(session)))
    }

    override suspend fun borrar() = withContext(dispatchers.io) { storage.borrar(CLAVE) }

    private companion object {
        const val CLAVE = "sesion"
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * La forma de la sesion **en disco**, separada de la del dominio.
 *
 * Es la misma idea que separar los DTO de red del dominio: lo guardado tiene
 * que poder leerse dentro de un ano, y atarlo a una clase que cambia con la app
 * convierte cualquier refactor en una sesion perdida.
 */
@Serializable
private data class SessionGuardada(
    val acceso: String,
    val refresco: String,
    val caducaEn: Long,
    val usuario: String,
    val email: String? = null,
) {
    fun aSession() = Session(acceso, refresco, caducaEn, usuario, email)

    companion object {
        fun de(s: Session) = SessionGuardada(
            acceso = s.accessToken,
            refresco = s.refreshToken,
            caducaEn = s.expiresAtEpochSeconds,
            usuario = s.userId,
            email = s.email,
        )
    }
}
