package com.sebastiannarvaez.pokedex.core

/**
 * El error tal y como lo pinta una pantalla.
 *
 * `data class` y no jerarquia sellada: este tipo **si** cruza a Swift, y alli
 * una `sealed` pierde la exhaustividad. Lo que llega son datos listos para
 * pintar, no una decision que cada interfaz tenga que volver a tomar.
 *
 * Las dos apps comparten el mapeo. Si no, una diria «sin conexion» y la otra
 * «no hay internet», y nadie sabria por que.
 */
data class UiError(
    val titulo: String,
    val detalle: String,
    val sePuedeReintentar: Boolean,
)

fun AppError.aUiError(): UiError = when (this) {
    AppError.SinConexion -> UiError(
        titulo = "Sin conexión",
        detalle = "Revisa tu red e inténtalo otra vez.",
        sePuedeReintentar = true,
    )
    AppError.Lento -> UiError(
        titulo = "La respuesta tardó demasiado",
        detalle = "El servidor no contestó a tiempo.",
        sePuedeReintentar = true,
    )
    is AppError.Servidor -> UiError(
        titulo = "PokeAPI no responde",
        detalle = "El servidor devolvió un error $codigo.",
        sePuedeReintentar = esReintentable,
    )
    is AppError.Inesperado -> UiError(
        titulo = "Algo se rompió",
        detalle = causa,
        sePuedeReintentar = false,
    )
}
