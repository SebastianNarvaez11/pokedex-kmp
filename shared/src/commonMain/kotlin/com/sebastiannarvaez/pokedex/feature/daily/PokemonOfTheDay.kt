package com.sebastiannarvaez.pokedex.feature.daily

import kotlinx.datetime.LocalDate

/**
 * El Pokemon del dia, calculado y no sorteado.
 *
 * La regla es una funcion pura de la fecha: **no hace falta red, ni servidor,
 * ni guardar nada**. Dos dispositivos con la misma fecha muestran el mismo
 * Pokemon, y la notificacion se puede programar con una semana de antelacion
 * sin consultar nada.
 *
 * Es la diferencia entre una feature que necesita backend y una que no.
 */
object PokemonOfTheDay {

    /** Cuantos Pokemon numerados de forma continua hay en PokeAPI. */
    const val TOTAL = 1025

    /**
     * Un numero entre 1 y TOTAL a partir de la fecha.
     *
     * No se usa `Random`: una funcion aleatoria daria un Pokemon distinto en
     * cada llamada, y el del dia tiene que ser el mismo a las nueve que a las
     * once. El dia epoch pasa por una mezcla sencilla para que dias seguidos no
     * den numeros seguidos.
     */
    fun idParaFecha(fecha: LocalDate): Int {
        val dias = fecha.toEpochDays().toLong()
        // Mezcla de enteros de Thomas Wang, suficiente para que la secuencia no
        // se note a simple vista y estable entre plataformas.
        var x = dias * 2_654_435_761L
        x = x xor (x shr 16)
        x *= 2_246_822_519L
        x = x xor (x shr 13)
        val positivo = (x and 0x7FFF_FFFFL)
        return (positivo % TOTAL).toInt() + 1
    }
}
