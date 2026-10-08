package com.sebastiannarvaez.pokedex.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sebastiannarvaez.pokedex.data.PokemonListRepository
import com.sebastiannarvaez.pokedex.domain.Pokemon
import kotlinx.coroutines.flow.Flow

class PokemonListViewModel internal constructor(
    repository: PokemonListRepository,
) : ViewModel() {

    /**
     * La lista, leida de la base y llenada desde la red por el mediador. El
     * ViewModel no sabe nada de eso: recibe paginas, como antes.
     *
     * `cachedIn(viewModelScope)` es lo que hace que al girar el telefono la
     * lista no vuelva a pedirse entera: las paginas ya cargadas sobreviven
     * mientras viva el ViewModel.
     *
     * Sin el, ademas, un `Flow<PagingData>` no se puede recolectar dos veces.
     */
    val pokemon: Flow<PagingData<Pokemon>> = repository.pokemon().cachedIn(viewModelScope)
}
