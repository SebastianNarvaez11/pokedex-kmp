package com.sebastiannarvaez.pokedex.feature.search

import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.domain.PokemonRef

data class SearchUiState(
    val consulta: String = "",
    val resultados: List<PokemonRef> = emptyList(),
    val buscando: Boolean = false,
    val error: UiError? = null,
) {
    /** Sin escribir nada todavia: la pantalla sugiere, no dice «sin resultados». */
    val enReposo: Boolean get() = consulta.isBlank()

    val sinResultados: Boolean get() = !enReposo && !buscando && error == null && resultados.isEmpty()
}
