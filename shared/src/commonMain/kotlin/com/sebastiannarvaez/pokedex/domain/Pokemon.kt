package com.sebastiannarvaez.pokedex.domain

/**
 * Un Pokemon tal y como lo necesita la interfaz, no tal y como lo devuelve la
 * API.
 *
 * Es el modelo de dominio: no sabe de JSON, ni de columnas, ni de que existan
 * dos apps. Las clases que hablan con PokeAPI llegan en la leccion de red, y
 * son otras: traducir de una a otra es trabajo del repositorio.
 */
data class Pokemon(
    val id: Int,
    val name: String,
    val types: List<PokemonType>,
) {
    /**
     * La ilustracion oficial. PokeAPI no la devuelve en el listado, asi que se
     * construye a partir del identificador.
     */
    val artworkUrl: String
        get() = "$ARTWORK_BASE/$id.png"

    companion object {
        private const val ARTWORK_BASE =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork"
    }
}
