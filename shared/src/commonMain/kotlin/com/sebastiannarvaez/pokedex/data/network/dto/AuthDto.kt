package com.sebastiannarvaez.pokedex.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** La sesion tal y como la devuelve Supabase. */
@Serializable
internal data class SessionDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserDto? = null,
)

@Serializable
internal data class UserDto(
    val id: String,
    val email: String? = null,
)

/**
 * El error de Supabase, con **todos** los campos opcionales.
 *
 * La forma no es la misma en todos los endpoints: unos devuelven `error` y
 * `error_description`, otros `msg`, otros `message` con `error_code`. Un DTO
 * tolerante evita que un fallo de red se convierta en un fallo de parseo, que
 * es mucho peor de diagnosticar.
 */
@Serializable
internal data class AuthErrorDto(
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val msg: String? = null,
    val message: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
) {
    /** El primero que venga relleno, que es lo unico que se puede ensenar. */
    val mensaje: String?
        get() = errorDescription ?: msg ?: message ?: error
}
