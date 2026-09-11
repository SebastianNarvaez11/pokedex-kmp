package com.sebastiannarvaez.pokedex.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PokemonDetailDto(
    val id: Int,
    val name: String,
    /** En decimetros, no en metros. PokeAPI mide raro. */
    val height: Int = 0,
    /** En hectogramos. */
    val weight: Int = 0,
    val types: List<TypeSlotDto> = emptyList(),
)

@Serializable
internal data class TypeSlotDto(
    val slot: Int = 0,
    val type: NamedRefDto,
)

@Serializable
internal data class NamedRefDto(val name: String)
