package com.sebastiannarvaez.pokedex.feature.detail

import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.domain.PokemonDetail

/**
 * Todo lo que la ficha necesita, en un objeto.
 *
 * Tres banderas y dos datos en vez de una jerarquia sellada: cruza a Swift, y
 * alli una sellada pierde la exhaustividad.
 */
data class PokemonDetailUiState(
    val cargando: Boolean = true,
    val detalle: PokemonDetail? = null,
    val error: UiError? = null,
)
