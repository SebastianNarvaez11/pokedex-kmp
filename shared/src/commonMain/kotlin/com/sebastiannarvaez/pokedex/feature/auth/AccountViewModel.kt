package com.sebastiannarvaez.pokedex.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.data.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lo que pasa **dentro** de la cuenta: cambiar la contrasena y borrarla.
 *
 * Aparte de `AuthViewModel` porque vive en otro sitio de la app —los ajustes, y
 * la pantalla de contrasena nueva— y porque nada de esto tiene sentido sin
 * sesion. Juntarlos daria un ViewModel con dos mitades que nunca se usan a la
 * vez.
 */
data class AccountState(
    val trabajando: Boolean = false,
    val error: UiError? = null,
    val passwordCambiada: Boolean = false,
)

class AccountViewModel internal constructor(
    private val repository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    fun cambiarPassword(nueva: String) {
        if (nueva.length < AuthFormState.MINIMO_PASSWORD || _state.value.trabajando) return
        _state.value = AccountState(trabajando = true)
        viewModelScope.launch {
            _state.value = try {
                repository.cambiarPassword(nueva)
                AccountState(passwordCambiada = true)
            } catch (e: Throwable) {
                AccountState(error = e.aUiErrorDeAuth())
            }
        }
    }

    fun borrarCuenta() {
        if (_state.value.trabajando) return
        _state.value = AccountState(trabajando = true)
        viewModelScope.launch {
            _state.value = try {
                repository.borrarCuenta()
                // No se pone ningun estado de exito: al borrar la cuenta se
                // cierra la sesion, y la app vuelve sola a la pantalla de
                // entrada. Anunciarlo seria un mensaje que nadie llega a leer.
                AccountState()
            } catch (e: Throwable) {
                AccountState(error = e.aUiErrorDeAuth())
            }
        }
    }

    fun descartarError() {
        _state.value = _state.value.copy(error = null)
    }
}
