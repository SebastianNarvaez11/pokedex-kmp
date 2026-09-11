package com.sebastiannarvaez.pokedex.feature.list

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebastiannarvaez.pokedex.core.AppDispatchers
import org.koin.mp.KoinPlatform

/**
 * Lo unico que Swift necesita saber de la lista.
 *
 * Devuelve el presentador ya montado sobre un ViewModel que vive en el almacen
 * de la pantalla: Koin lo construye, el almacen lo entierra.
 */
fun pokemonListPresenter(owner: ViewModelStoreOwner): PokemonListPresenter {
    val koin = KoinPlatform.getKoin()
    val viewModel = ViewModelProvider.create(
        owner = owner,
        factory = viewModelFactory { initializer { koin.get<PokemonListViewModel>() } },
    )[PokemonListViewModel::class]

    return PokemonListPresenter(viewModel, koin.get<AppDispatchers>())
}
