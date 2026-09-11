package com.sebastiannarvaez.pokedex.feature.settings

/**
 * Las preferencias de la app.
 *
 * `Tema` es un `enum` y no un booleano de «modo oscuro» porque son **tres**
 * estados, no dos: claro, oscuro y «lo que diga el sistema». Con un booleano,
 * el tercero no se puede representar y acaba siendo un segundo booleano que
 * nadie recuerda por que existe.
 */
enum class Tema { SISTEMA, CLARO, OSCURO }

data class Settings(
    val tema: Tema = Tema.SISTEMA,
    /** Ordenar los favoritos por lo mas reciente o por numero. */
    val favoritosPorNumero: Boolean = false,
)
