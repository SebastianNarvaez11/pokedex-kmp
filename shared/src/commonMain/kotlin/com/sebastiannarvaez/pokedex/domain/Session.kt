package com.sebastiannarvaez.pokedex.domain

/**
 * La sesion del usuario.
 *
 * Guarda **cuando caduca**, no cuanto dura: un `expires_in` de 3600 solo sirve
 * en el instante en que llega, y guardarlo tal cual convierte cualquier
 * comprobacion posterior en una mentira.
 */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val userId: String,
    val email: String?,
) {
    /**
     * Si conviene refrescar ya.
     *
     * Con margen: un token que caduca dentro de treinta segundos ya no sirve
     * para una peticion que tarda dos.
     */
    fun caducaAntesDe(ahoraEpochSeconds: Long, margenSegundos: Long = 60): Boolean =
        ahoraEpochSeconds + margenSegundos >= expiresAtEpochSeconds
}
