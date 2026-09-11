package com.sebastiannarvaez.pokedex.domain

/**
 * Un favorito, ya en lenguaje de la app.
 *
 * Tiene lo justo para pintar una fila **sin red**: numero, nombre y la
 * ilustracion, que se construye del numero.
 */
data class FavoritePokemon(val id: Int, val name: String) {
    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}
