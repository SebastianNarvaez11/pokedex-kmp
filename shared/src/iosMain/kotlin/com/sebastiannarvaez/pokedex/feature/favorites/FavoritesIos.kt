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
 * Los identificadores de los favoritos. Swift los recibe como `Set<KotlinInt>`
 * y los convierte a `Set<Int32>` una vez por cambio.
 */
@NativeCoroutinesState
val FavoritesViewModel.favoriteIdsForIos: StateFlow<Set<Int>>
    get() = favoriteIds
