package com.sebastiannarvaez.pokedex.feature.detail

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/**
 * La ficha para iOS.
 *
 * El identificador no lo sabe el grafo, lo trae la navegacion: por eso se pasa
 * con `parametersOf` en vez de inyectarlo.
 */
fun pokemonDetailViewModel(owner: ViewModelStoreOwner, pokemonId: Int): PokemonDetailViewModel {
    val koin = KoinPlatform.getKoin()
    return ViewModelProvider.create(
        owner = owner,
        factory = viewModelFactory {
            initializer { koin.get<PokemonDetailViewModel> { parametersOf(pokemonId) } }
        },
    )[PokemonDetailViewModel::class]
}

@NativeCoroutinesState
val PokemonDetailViewModel.uiStateForIos: StateFlow<PokemonDetailUiState>
    get() = uiState
