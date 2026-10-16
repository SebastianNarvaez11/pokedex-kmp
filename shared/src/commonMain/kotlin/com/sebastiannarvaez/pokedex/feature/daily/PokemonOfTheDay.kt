package com.sebastiannarvaez.pokedex.feature.daily

import kotlinx.datetime.LocalDate   // «un día del calendario», sin hora ni zona

/**
 * El Pokémon del día, calculado y no sorteado.
 * Función pura de la fecha: no hace falta red, ni servidor, ni guardar nada.
 */
object PokemonOfTheDay {

    /** Cuántos Pokémon numerados de forma continua hay en PokeAPI. */
    const val TOTAL = 1025

    /** Un número entre 1 y TOTAL a partir de la fecha. */
    fun idParaFecha(fecha: LocalDate): Int {
        // Los días transcurridos desde el 1 de enero de 1970. Ya es un Long (64 bits).
        val dias = fecha.toEpochDays()
        // Mezcla de bits, como las de las funciones hash: multiplica, desplaza
        // (shr) y combina (xor). No hace falta seguir la cuenta: solo sirve para
        // que días seguidos no den números seguidos.
        var x = dias * 2_654_435_761L
        x = x xor (x shr 16)
        x *= 2_246_822_519L
        x = x xor (x shr 13)
        // `and` con esta máscara se queda con 31 bits: el resultado nunca es negativo.
        val positivo = (x and 0x7FFF_FFFFL)
        // Resto de dividir entre 1025 (0..1024) y +1 para que sea 1..1025.
        return (positivo % TOTAL).toInt() + 1
    }
}
