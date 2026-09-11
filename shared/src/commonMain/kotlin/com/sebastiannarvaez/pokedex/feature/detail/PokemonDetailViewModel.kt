package com.sebastiannarvaez.pokedex.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.core.aAppError
import com.sebastiannarvaez.pokedex.core.aUiError
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PokemonDetailViewModel internal constructor(
    private val repository: PokemonRepository,
    private val pokemonId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PokemonDetailUiState())
    val uiState: StateFlow<PokemonDetailUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        _uiState.value = PokemonDetailUiState(cargando = true)
        viewModelScope.launch {
            _uiState.value = try {
                PokemonDetailUiState(cargando = false, detalle = repository.detail(pokemonId))
            } catch (e: Throwable) {
                // `aAppError` relanza la cancelacion, asi que este catch no se
                // traga el cierre de la pantalla: solo atrapa fallos de verdad.
                PokemonDetailUiState(cargando = false, error = e.aAppError().aUiError())
            }
        }
    }
}
