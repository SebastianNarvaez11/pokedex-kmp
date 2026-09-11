package com.sebastiannarvaez.pokedex.domain

/**
 * Los dieciocho tipos del juego.
 *
 * Es un `enum` y no un `String` porque la interfaz pinta un color por tipo, y
 * un color por cadena de texto es un `when` con rama `else` que nadie revisa.
 * El precio se paga en Swift, donde un `enum` de Kotlin pierde la
 * exhaustividad: eso se cuenta en la leccion de tipos que cruzan.
 */
enum class PokemonType {
    NORMAL, FIGHTING, FLYING, POISON, GROUND, ROCK, BUG, GHOST, STEEL,
    FIRE, WATER, GRASS, ELECTRIC, PSYCHIC, ICE, DRAGON, DARK, FAIRY;

    companion object {
        /**
         * PokeAPI devuelve el tipo como texto en minusculas. Si aparece uno
         * que no conocemos, no se revienta: se ignora. Una API que anade un
         * tipo nuevo no deberia tumbar una app publicada.
         */
        fun deApi(nombre: String): PokemonType? =
            entries.firstOrNull { it.name.equals(nombre, ignoreCase = true) }
    }
}
