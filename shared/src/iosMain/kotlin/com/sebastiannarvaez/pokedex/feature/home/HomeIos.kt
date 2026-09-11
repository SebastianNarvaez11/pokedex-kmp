package com.sebastiannarvaez.pokedex.feature.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import org.koin.mp.KoinPlatform
import kotlinx.coroutines.flow.StateFlow

/**
 * Crea el ViewModel dentro del almacen que le pasa Swift.
 *
 * El tipo de retorno es concreto, no generico, a proposito: la funcion generica
 * de la receta oficial obliga a pelearse con `KClass` desde Swift. Una funcion
 * por pantalla es mas codigo, pero es codigo que se lee.
 */
fun homeViewModel(owner: ViewModelStoreOwner): HomeViewModel =
    ViewModelProvider.create(
        owner = owner,
        // Koin lo construye, el almacen lo entierra: cada uno hace lo suyo.
        factory = viewModelFactory { initializer { KoinPlatform.getKoin().get<HomeViewModel>() } },
    )[HomeViewModel::class]

/**
 * El estado, como algo que Swift puede recorrer.
 *
 * `@NativeCoroutinesState` y no `@NativeCoroutines`: ademas de la secuencia
 * asincrona, genera la propiedad con el valor actual, que es lo que evita que
 * la pantalla parpadee vacia en el primer fotograma.
 *
 * Y es una **propiedad de extension**, no una funcion: la anotacion solo se
 * puede aplicar a propiedades, y el compilador lo dice sin rodeos.
 */
@NativeCoroutinesState
val HomeViewModel.uiStateForIos: StateFlow<HomeUiState>
    get() = uiState
