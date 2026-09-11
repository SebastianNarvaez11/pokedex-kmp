package com.sebastiannarvaez.pokedex.feature.search

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.StateFlow
import org.koin.mp.KoinPlatform

fun pokemonSearchViewModel(owner: ViewModelStoreOwner): PokemonSearchViewModel {
    val koin = KoinPlatform.getKoin()
    return ViewModelProvider.create(
        owner = owner,
        factory = viewModelFactory { initializer { koin.get<PokemonSearchViewModel>() } },
    )[PokemonSearchViewModel::class]
}

@NativeCoroutinesState
val PokemonSearchViewModel.uiStateForIos: StateFlow<SearchUiState>
    get() = uiState
