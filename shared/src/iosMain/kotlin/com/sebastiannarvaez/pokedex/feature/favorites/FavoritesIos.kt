package com.sebastiannarvaez.pokedex.feature.favorites

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.StateFlow
import org.koin.mp.KoinPlatform

fun favoritesViewModel(owner: ViewModelStoreOwner): FavoritesViewModel {
    val koin = KoinPlatform.getKoin()
    return ViewModelProvider.create(
        owner = owner,
        factory = viewModelFactory { initializer { koin.get<FavoritesViewModel>() } },
    )[FavoritesViewModel::class]
}

@NativeCoroutinesState
val FavoritesViewModel.uiStateForIos: StateFlow<FavoritesUiState>
    get() = uiState

/**
 * Un `Set<Int>` cruza a Swift como `NSSet` de `KotlinInt`, que es incomodo de
 * consultar. Se expone como lista y Swift la convierte a `Set<Int32>` una vez.
 */
@NativeCoroutinesState
val FavoritesViewModel.favoriteIdsForIos: StateFlow<List<Int>>
    get() = favoriteIdsList
