package com.sebastiannarvaez.pokedex.domain

/**
 * Lo minimo para buscar: un numero y un nombre.
 *
 * No es un `Pokemon` porque no tiene tipos, y fingir que los tiene obligaria a
 * pedir el detalle de los mil trescientos solo para poder escribir en un campo
 * de texto. La ilustracion si sale gratis: se construye del identificador.
 */
data class PokemonRef(val id: Int, val name: String) {
    val artworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}
