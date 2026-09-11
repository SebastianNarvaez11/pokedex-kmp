package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.domain.Session

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
