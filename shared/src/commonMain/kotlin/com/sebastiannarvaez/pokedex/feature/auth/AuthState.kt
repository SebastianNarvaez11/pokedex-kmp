package com.sebastiannarvaez.pokedex.feature.auth

import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.domain.Session

/**
 * Quien esta usando la app.
 *
 * `comprobando` no es un estado de adorno: al arrancar hay que leer el token
 * guardado, y eso tarda. Sin este estado, la app ensenaria la pantalla de
 * entrada durante un instante a alguien que ya tenia sesion, que es de los
 * parpadeos que mas molestan.
 */
data class AuthState(
    val comprobando: Boolean = true,
    val session: Session? = null,
) {
    val haySesion: Boolean get() = session != null
}

/** El estado de una pantalla de entrada o registro. */
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val enviando: Boolean = false,
    val error: UiError? = null,
    val correoEnviado: Boolean = false,
) {
    /**
     * El constructor vacio, para Swift.
     *
     * Los argumentos por defecto de Kotlin **no cruzan** a Objective-C: alli
     * solo llega el constructor con todos los parametros. Sin esto, cada campo
     * nuevo rompe la compilacion de Swift con «missing argument for
     * parameter», que fue exactamente lo que paso al anadir `correoEnviado`.
     */
    constructor() : this(email = "")

    /**
     * Validacion minima, en el nucleo y no en cada pantalla.
     *
     * Si estuviera en la interfaz habria dos reglas distintas, y una de las dos
     * dejaria pasar algo que el servidor rechaza.
     */
    val emailValido: Boolean
        get() = email.contains('@') && email.substringAfter('@').contains('.')

    val passwordValida: Boolean get() = password.length >= MINIMO_PASSWORD

    val sePuedeEnviar: Boolean get() = emailValido && passwordValida && !enviando

    companion object {
        /** El minimo que exige Supabase por defecto. */
        const val MINIMO_PASSWORD = 6
    }
}
