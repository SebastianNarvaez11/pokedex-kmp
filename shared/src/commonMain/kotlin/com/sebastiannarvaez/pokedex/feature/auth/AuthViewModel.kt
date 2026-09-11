package com.sebastiannarvaez.pokedex.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.core.aAppError
import com.sebastiannarvaez.pokedex.core.aUiError
import com.sebastiannarvaez.pokedex.data.SessionRepository
import com.sebastiannarvaez.pokedex.data.network.AuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * El ViewModel de entrar, registrarse y salir.
 *
 * Publica dos flujos distintos a proposito: `state` es quien eres —lo mira toda
 * la app para decidir que pantallas hay— y `form` es lo que estas escribiendo
 * ahora mismo, que solo le importa a la pantalla de entrada.
 */
class AuthViewModel internal constructor(
    private val repository: SessionRepository,
) : ViewModel() {

    val state: StateFlow<AuthState> = repository.state

    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    init {
        viewModelScope.launch { repository.restaurar() }
    }

    fun escribirEmail(valor: String) {
        _form.value = _form.value.copy(email = valor, error = null)
    }

    fun escribirPassword(valor: String) {
        _form.value = _form.value.copy(password = valor, error = null)
    }

    fun entrar() = enviar { repository.entrar(it.email, it.password) }

    fun registrar() = enviar { repository.registrar(it.email, it.password) }

    fun salir() {
        viewModelScope.launch { repository.salir() }
    }

    fun descartarError() {
        _form.value = _form.value.copy(error = null)
    }

    private fun enviar(accion: suspend (AuthFormState) -> Unit) {
        val actual = _form.value
        if (!actual.sePuedeEnviar) return
        _form.value = actual.copy(enviando = true, error = null)
        viewModelScope.launch {
            try {
                accion(actual)
                // Al entrar, el formulario se vacia: la contrasena no debe
                // quedarse en memoria despues de usarla.
                _form.value = AuthFormState()
            } catch (e: Throwable) {
                _form.value = _form.value.copy(enviando = false, error = e.aUiErrorDeAuth())
            }
        }
    }
}

/**
 * Un 400 de Supabase no es «el servidor falla»: es «esa contrasena no es».
 *
 * El mapeo general trata cualquier 4xx como error de servidor, que aqui seria
 * mentir al usuario. Por eso la excepcion propia tiene su propio camino, y el
 * resto cae en el mapeo de siempre.
 */
internal fun Throwable.aUiErrorDeAuth(): UiError = when (this) {
    is AuthException -> UiError(
        titulo = "No se pudo continuar",
        detalle = message ?: "Revisa el correo y la contraseña.",
        sePuedeReintentar = false,
    )
    else -> aAppError().aUiError()
}
