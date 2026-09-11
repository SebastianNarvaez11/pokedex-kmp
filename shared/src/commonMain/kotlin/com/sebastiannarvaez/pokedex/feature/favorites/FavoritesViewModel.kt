package com.sebastiannarvaez.pokedex.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.data.FavoritesRepository
import com.sebastiannarvaez.pokedex.domain.FavoritePokemon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoritos: List<FavoritePokemon> = emptyList(),
    val cargando: Boolean = true,
) {
    val vacia: Boolean get() = !cargando && favoritos.isEmpty()
}

class FavoritesViewModel internal constructor(
    private val repository: FavoritesRepository,
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = repository.observeFavorites()
        .map { FavoritesUiState(favoritos = it, cargando = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    /** Los identificadores, para que la lista pinte el corazon sin recargarse. */
    val favoriteIds: StateFlow<Set<Int>> = repository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggle(id: Int, name: String) {
        viewModelScope.launch { repository.toggle(id, name) }
    }

    fun vaciar() {
        viewModelScope.launch { repository.clear() }
    }
}
