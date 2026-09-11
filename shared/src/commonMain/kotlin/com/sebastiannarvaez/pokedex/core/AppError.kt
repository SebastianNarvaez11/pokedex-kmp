package com.sebastiannarvaez.pokedex.core

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * Los cuatro errores que la app sabe contar.
 *
 * Es una `sealed interface` porque vive **puertas adentro**: el `when` que la
 * traduce esta en Kotlin y el compilador obliga a cubrir los casos. Lo que sale
 * hacia las pantallas es un `UiError`, que es una `data class`, porque una
 * jerarquia sellada pierde la exhaustividad al cruzar a Swift.
 */
sealed interface AppError {
    /** No hay red, o el servidor no contesta. */
    data object SinConexion : AppError

    /** El servidor contesto, pero tardo demasiado. */
    data object Lento : AppError

    /** El servidor dijo que no: 4xx o 5xx. */
    data class Servidor(val codigo: Int) : AppError

    /** Cualquier otra cosa. Se registra con su causa. */
    data class Inesperado(val causa: String) : AppError
}

/**
 * Traduce una excepcion a algo que se pueda pintar.
 *
 * La cancelacion **no se traduce**: se vuelve a lanzar. Tragarsela rompe la
 * concurrencia estructurada, y el sintoma es una corrutina que sigue viva
 * despues de que su pantalla desaparezca.
 */
fun Throwable.aAppError(): AppError = when (this) {
    is CancellationException -> throw this
    is HttpRequestTimeoutException, is ConnectTimeoutException -> AppError.Lento
    is ResponseException -> AppError.Servidor(response.status.value)
    is IOException -> AppError.SinConexion
    else -> AppError.Inesperado(message ?: this::class.simpleName.orEmpty())
}

/** Si vale la pena ofrecer un boton de reintentar. */
val AppError.esReintentable: Boolean
    get() = when (this) {
        AppError.SinConexion, AppError.Lento -> true
        is AppError.Servidor -> codigo >= HttpStatusCode.InternalServerError.value
        is AppError.Inesperado -> false
    }
