package com.sebastiannarvaez.pokedex.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.core.aAppError
import com.sebastiannarvaez.pokedex.core.aUiError
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PokemonSearchViewModel internal constructor(
    private val repository: PokemonRepository,
) : ViewModel() {

    private val consulta = MutableStateFlow("")

    fun escribir(texto: String) {
        consulta.value = texto
    }

    fun limpiar() {
        consulta.value = ""
    }

    /**
     * El canal clasico de una busqueda, en cuatro operadores.
     *
     * - `debounce` espera a que el usuario deje de teclear. Sin el, escribir
     *   «pikachu» lanza siete busquedas y las seis primeras sobran.
     * - `distinctUntilChanged` ignora el texto que no cambia, como al mover el
     *   cursor o pegar lo mismo.
     * - `flatMapLatest` **cancela la busqueda anterior** al llegar una nueva.
     *   Con `flatMapMerge`, una respuesta lenta podria pisar a una posterior y
     *   la pantalla acabaria mostrando resultados de una consulta vieja.
     * - `stateIn` lo deja en un estado que la interfaz puede leer sin
     *   suscribirse a mano.
     */
    private val resultados: StateFlow<SearchUiState> = consulta
        .debounce { if (it.isBlank()) 0.milliseconds else ESPERA }
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { texto -> buscar(texto) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    /** El texto se publica al instante; los resultados van a su ritmo. */
    val uiState: StateFlow<SearchUiState> = combine(consulta, resultados) { texto, estado ->
        estado.copy(consulta = texto, buscando = estado.buscando || texto.trim() != estado.consulta.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    private fun buscar(texto: String) = flow {
        if (texto.isBlank()) {
            emit(SearchUiState(consulta = texto))
            return@flow
        }
        emit(SearchUiState(consulta = texto, buscando = true))
        try {
            val indice = repository.searchIndex()
            val normalizado = texto.lowercase()
            val encontrados = indice
                .filter { it.name.contains(normalizado) }
                // Lo que empieza por lo escrito, primero: buscar «char» debe
                // dar Charmander antes que Ampharos.
                .sortedWith(compareByDescending<com.sebastiannarvaez.pokedex.domain.PokemonRef> {
                    it.name.startsWith(normalizado)
                }.thenBy { it.id })
                .take(MAXIMO_RESULTADOS)
            emit(SearchUiState(consulta = texto, resultados = encontrados))
        } catch (e: Throwable) {
            emit(SearchUiState(consulta = texto, error = e.aAppError().aUiError()))
        }
    }

    private companion object {
        val ESPERA = 300.milliseconds

        /** Nadie recorre mil resultados: mas alla de esto, que afine. */
        const val MAXIMO_RESULTADOS = 50
    }
}
