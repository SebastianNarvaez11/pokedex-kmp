package com.sebastiannarvaez.pokedex.domain

/**
 * La ficha completa, que solo hace falta al abrir un Pokemon.
 *
 * Es un tipo distinto de `Pokemon` y no una version con campos nulos: el
 * listado nunca tiene esta informacion, y un modelo con la mitad de los campos
 * vacios obliga a preguntar en cada pantalla si estan o no.
 */
data class PokemonDetail(
    val id: Int,
    val name: String,
    val types: List<PokemonType>,
    /** En centimetros. PokeAPI lo da en decimetros. */
    val heightCm: Int,
    /** En gramos. PokeAPI lo da en hectogramos. */
    val weightG: Int,
    /** «Pokemon Semilla». Viene de la especie, no del Pokemon. */
    val genus: String,
    val description: String,
    val stats: List<PokemonStat>,
    val isLegendary: Boolean,
) {
    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}

data class PokemonStat(val kind: StatKind, val value: Int) {
    companion object {
        /** El maximo que alcanza una estadistica base en el juego. */
        const val MAXIMO = 255
    }
}

enum class StatKind {
    HP, ATTACK, DEFENSE, SPECIAL_ATTACK, SPECIAL_DEFENSE, SPEED;

    companion object {
        /** PokeAPI usa guiones: `special-attack`. */
        fun deApi(nombre: String): StatKind? = when (nombre) {
            "hp" -> HP
            "attack" -> ATTACK
            "defense" -> DEFENSE
            "special-attack" -> SPECIAL_ATTACK
            "special-defense" -> SPECIAL_DEFENSE
            "speed" -> SPEED
            else -> null
        }
    }
}
