package com.sebastiannarvaez.pokedex.data.network.dto

import kotlinx.serialization.Serializable

/**
 * La respuesta del listado de PokeAPI, tal cual llega.
 *
 * Es un DTO y no el modelo de dominio: describe el JSON, no lo que la app
 * necesita. Mezclarlos ata la interfaz a la forma de una API que no controlas,
 * y el dia que la API cambie un nombre de campo hay que tocar pantallas.
 */
@Serializable
internal data class PokemonPageDto(
    val count: Int,
    val next: String? = null,
    val results: List<PokemonRefDto> = emptyList(),
)

/**
 * El listado no trae ni tipos ni imagen: solo el nombre y una URL. El
 * identificador hay que sacarlo de esa URL, que termina en `/pokemon/25/`.
 */
@Serializable
internal data class PokemonRefDto(
    val name: String,
    val url: String,
) {
    val id: Int?
        get() = url.trimEnd('/').substringAfterLast('/').toIntOrNull()
}
