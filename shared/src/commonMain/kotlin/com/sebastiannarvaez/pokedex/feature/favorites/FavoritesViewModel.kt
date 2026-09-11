package com.sebastiannarvaez.pokedex.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.data.FavoritesRepository
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import com.sebastiannarvaez.pokedex.domain.FavoritePokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
    settings: SettingsRepository,
) : ViewModel() {

    /**
     * La lista y el ajuste de orden, combinados.
     *
     * El orden se aplica aqui y no en la consulta SQL por una razon practica:
     * cambiar el ajuste no deberia obligar a la base a reconsultar. Con
     * `combine`, el ultimo valor de cada flujo se reordena en memoria, que para
     * una lista de favoritos es inmediato.
     */
    val uiState: StateFlow<FavoritesUiState> =
        combine(repository.observeFavorites(), settings.settings) { favoritos, ajustes ->
            FavoritesUiState(
                favoritos = if (ajustes.favoritosPorNumero) favoritos.sortedBy { it.id } else favoritos,
                cargando = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    /** Los identificadores, para que la lista pinte el corazon sin recargarse. */
    val favoriteIds: StateFlow<Set<Int>> = repository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** La misma informacion como lista, para que cruce comoda a Swift. */
    internal val favoriteIdsList: StateFlow<List<Int>> = repository.observeFavoriteIds()
        .map { it.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(id: Int, name: String, primaryType: PokemonType? = null) {
        viewModelScope.launch { repository.toggle(id, name, primaryType) }
    }

    fun vaciar() {
        viewModelScope.launch { repository.clear() }
    }
}
