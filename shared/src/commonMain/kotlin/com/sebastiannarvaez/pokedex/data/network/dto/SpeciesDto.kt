package com.sebastiannarvaez.pokedex.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * La especie: lo que en el juego comparten un Pokemon y sus evoluciones.
 *
 * Aqui viven la descripcion y el genero, que PokeAPI **no** devuelve en
 * `/pokemon/{id}`. Por eso el detalle son dos peticiones y no una.
 */
@Serializable
internal data class SpeciesDto(
    val id: Int,
    @SerialName("is_legendary") val isLegendary: Boolean = false,
    @SerialName("is_mythical") val isMythical: Boolean = false,
    @SerialName("flavor_text_entries") val flavorTexts: List<FlavorTextDto> = emptyList(),
    val genera: List<GenusDto> = emptyList(),
)

@Serializable
internal data class FlavorTextDto(
    @SerialName("flavor_text") val text: String,
    val language: NamedRefDto,
)

@Serializable
internal data class GenusDto(
    val genus: String,
    val language: NamedRefDto,
)
