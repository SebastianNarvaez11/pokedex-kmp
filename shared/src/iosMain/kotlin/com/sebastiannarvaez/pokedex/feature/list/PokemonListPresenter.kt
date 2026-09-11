package com.sebastiannarvaez.pokedex.feature.list

import androidx.paging.LoadState
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.core.aAppError
import com.sebastiannarvaez.pokedex.core.aUiError
import com.sebastiannarvaez.pokedex.domain.Pokemon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * El puente entre Paging y SwiftUI.
 *
 * `paging-compose` hace esto mismo para Android. En iOS no hay nada, asi que
 * se escribe: `PagingDataPresenter` es la clase de `commonMain` pensada
 * exactamente para esto —«connects the UI layer to the underlying Paging
 * operations»— y lo que falta es traducir sus eventos a un estado plano.
 *
 * Swift no llama nunca a Paging: solo lee `state` y avisa de por donde va
 * mirando con `loadAround`.
 */
class PokemonListPresenter internal constructor(
    private val viewModel: PokemonListViewModel,
    // Se inyecta en vez de usar `Dispatchers.Main` a pelo para poder probarlo:
    // `PagingDataPresenter` exige que sus eventos se procesen siempre en el
    // mismo hilo, y en un test ese hilo es el de prueba.
    private val dispatchers: AppDispatchers,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private val _state = MutableStateFlow(PagedListState(cargando = true))

    @NativeCoroutinesState
    val state: StateFlow<PagedListState> = _state.asStateFlow()

    // `mainContext` hay que pasarlo: por defecto es `Dispatchers.Main`, que en
    // un test de Kotlin/Native no existe salvo que se instale a mano. Sin esto
    // el presentador no emite nada y el test muere por tiempo de espera, sin
    // decir por que.
    private val presenter = object : PagingDataPresenter<Pokemon>(mainContext = dispatchers.main) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<Pokemon>) {
            // Cualquier evento —insercion, borrado, recarga— se resuelve igual:
            // se vuelve a leer la instantanea entera. Para una lista de unos
            // cientos de elementos es mas barato que llevar las diferencias a
            // mano, y no hay forma de equivocarse.
            publicar()
        }
    }

    init {
        scope.launch {
            viewModel.pokemon.collect { presenter.collectFrom(it) }
        }
        scope.launch {
            presenter.loadStateFlow.collect { publicar() }
        }
    }

    /** Avisa de que la interfaz esta mirando esa posicion, para pedir mas. */
    fun loadAround(index: Int) {
        if (index in 0 until presenter.size) presenter[index]
    }

    fun retry() = presenter.retry()

    fun refresh() = presenter.refresh()

    /** Hay que llamarlo al desaparecer la pantalla, o las corrutinas siguen. */
    fun close() = scope.cancel()

    private fun publicar() {
        val estados = presenter.loadStateFlow.value
        val refresh = estados?.refresh
        val append = estados?.append
        val items = presenter.snapshot().items

        _state.value = PagedListState(
            items = items,
            cargando = refresh is LoadState.Loading,
            cargandoMas = append is LoadState.Loading,
            error = (refresh as? LoadState.Error)?.error?.aAppError()?.aUiError(),
            errorAlAmpliar = (append as? LoadState.Error)?.error?.aAppError()?.aUiError(),
        )
    }
}
