package com.sebastiannarvaez.pokedex.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel internal constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun cambiarTema(tema: Tema) {
        viewModelScope.launch { repository.cambiarTema(tema) }
    }

    fun cambiarOrden(porNumero: Boolean) {
        viewModelScope.launch { repository.cambiarOrden(porNumero) }
    }
}
