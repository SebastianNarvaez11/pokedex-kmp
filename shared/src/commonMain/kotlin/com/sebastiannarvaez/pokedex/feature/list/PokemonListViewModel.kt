package com.sebastiannarvaez.pokedex.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.domain.Pokemon
import kotlinx.coroutines.flow.Flow

class PokemonListViewModel internal constructor(
    repository: PokemonRepository,
) : ViewModel() {

    /**
     * `cachedIn(viewModelScope)` es lo que hace que al girar el telefono la
     * lista no vuelva a pedirse entera: las paginas ya cargadas sobreviven
     * mientras viva el ViewModel.
     *
     * Sin el, ademas, un `Flow<PagingData>` no se puede recolectar dos veces.
     */
    val pokemon: Flow<PagingData<Pokemon>> = Pager(
        config = PagingConfig(
            // Veinte por pagina: cada una cuesta una peticion de listado mas
            // una por Pokemon, asi que subirlo multiplica el trafico.
            pageSize = PAGE_SIZE,
            // Cuantos quedan por delante antes de pedir la siguiente pagina.
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { PokemonPagingSource(repository) },
    ).flow.cachedIn(viewModelScope)

    companion object {
        const val PAGE_SIZE = 20
    }
}
