package com.sebastiannarvaez.pokedex.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.Pokedex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * El mismo ViewModel para las dos apps.
 *
 * `androidx.lifecycle.ViewModel` dejo de ser solo de Android: desde la 2.8.0
 * es multiplataforma, y `viewModelScope` se cancela igual en las dos. Lo que
 * cambia es quien decide cuando muere: en Android lo hace el sistema, y en iOS
 * hay que escribir el duenno a mano.
 */
class HomeViewModel(pokedex: Pokedex) : ViewModel() {

    private val saludo = pokedex.greeting()

    /**
     * `WhileSubscribed(5_000)` es lo que apaga el trabajo caro cuando nadie
     * mira, con cinco segundos de gracia para no reiniciarlo al rotar la
     * pantalla. Sin eso, el latido seguiria contando en segundo plano.
     */
    val uiState: StateFlow<HomeUiState> = pokedex.heartbeat()
        .map { HomeUiState(greeting = saludo, heartbeat = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(greeting = saludo),
        )
}
