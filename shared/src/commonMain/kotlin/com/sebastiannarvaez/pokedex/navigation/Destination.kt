package com.sebastiannarvaez.pokedex.navigation

import kotlinx.serialization.Serializable

/**
 * Las pantallas de la app, como dato.
 *
 * Lo que se comparte es **el mapa**, no la navegacion. Android lo recorre con
 * Navigation 3 y iOS con `NavigationStack`, y ninguna de las dos librerias
 * asoma por aqui: este fichero es Kotlin puro.
 *
 * Compartirlo tiene dos pagos concretos. Uno, un enlace profundo se traduce una
 * sola vez y las dos apps abren la misma pantalla. Dos, anadir una pantalla
 * obliga a tocar un `when` en Kotlin, donde el compilador comprueba que no
 * falta ninguna.
 */
@Serializable
sealed interface Destination {

    @Serializable
    data object Lista : Destination

    @Serializable
    data class Detalle(val pokemonId: Int) : Destination

    /**
     * La direccion de esta pantalla.
     *
     * Sirve para compartir y para los enlaces profundos. Que viva junto al
     * `parse` no es casualidad: si se separan, uno cambia y el otro no.
     */
    fun toPath(): String = when (this) {
        Lista -> "/"
        is Detalle -> "/pokemon/$pokemonId"
    }

    companion object {

        /** El esquema propio, que funciona sin dominio ni cuenta de pago. */
        const val ESQUEMA = "pokedex"

        /** El dominio del enlace `https`, que si se puede compartir por chat. */
        const val HOST = "sebastiannarvaez11.github.io"

        /**
         * El prefijo de la ruta.
         *
         * GitHub Pages sirve las paginas de proyecto bajo el nombre del
         * repositorio, no en la raiz del dominio. Eso tiene una consecuencia
         * importante para los App Links de Android, y se cuenta en su leccion.
         */
        const val BASE_PATH = "/pokedex-kmp"

        /**
         * Traduce una URL entrante a una pantalla.
         *
         * Devuelve null en vez de lanzar: una URL que no entendemos no es un
         * error de programacion, es alguien pegando cualquier cosa.
         */
        fun parse(url: String): Destination? {
            val sinEsquema = url
                .substringAfter("://", missingDelimiterValue = url)
                .let { if (it.startsWith(HOST)) it.removePrefix(HOST) else it }
                .let { if (it.startsWith(BASE_PATH)) it.removePrefix(BASE_PATH) else it }
            val ruta = "/" + sinEsquema.trim('/').substringBefore('?')

            return when {
                ruta == "/" -> Lista
                ruta.startsWith("/pokemon/") ->
                    ruta.removePrefix("/pokemon/").toIntOrNull()?.let(::Detalle)
                else -> null
            }
        }
    }
}

/** El enlace que se comparte: `https`, para que sea pulsable en cualquier app. */
fun Destination.toShareUrl(): String =
    "https://${Destination.HOST}${Destination.BASE_PATH}${toPath()}"

/** El enlace interno, que abre la app sin pasar por el navegador. */
fun Destination.toDeepLink(): String = "${Destination.ESQUEMA}://${toPath().trimStart('/')}"
