package com.sebastiannarvaez.pokedex.feature.auth

import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.domain.Session

/**
 * Quien esta usando la app.
 *
 * `Desconocido` no es un estado de adorno: al arrancar hay que leer el token
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

/**
 * El estado de una pantalla de entrada o registro.
 *
 * Guarda lo que el usuario escribio, tal cual, y que campos ha **tocado** ya.
 * Los errores no se guardan: se calculan cada vez a partir de lo escrito, asi
 * no pueden quedarse viejos ni contradecir lo que hay en el campo.
 */
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    // Tocado = el usuario ya escribio algo en el campo. Antes de eso el error
    // no se ensena: nadie quiere ver «Escribe tu correo» en rojo nada mas abrir.
    val emailTocado: Boolean = false,
    val passwordTocado: Boolean = false,
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
     * Las reglas, en el nucleo y no en cada pantalla.
     *
     * Si estuvieran en la interfaz habria dos reglas distintas, y una de las
     * dos dejaria pasar algo que el servidor rechaza. Cada una devuelve el
     * motivo (un `enum`), o `null` si no hay nada que decir.
     */
    fun validarEmail(): ErrorEmail? = when {
        email.isBlank() -> ErrorEmail.VACIO
        !email.contains('@') || !email.substringAfter('@').contains('.') -> ErrorEmail.NO_VALIDO
        else -> null
    }

    fun validarPassword(): ErrorPassword? = when {
        password.isEmpty() -> ErrorPassword.VACIA
        password.length < MINIMO_PASSWORD -> ErrorPassword.CORTA
        else -> null
    }

    // Lo que ve la pantalla: el error SOLO si el campo esta tocado; si no, null.
    val errorEmail: ErrorEmail? get() = if (emailTocado) validarEmail() else null
    val errorPassword: ErrorPassword? get() = if (passwordTocado) validarPassword() else null

    val emailValido: Boolean get() = validarEmail() == null
    val passwordValida: Boolean get() = validarPassword() == null

    // El boton solo se activa con el formulario valido, tocado o no.
    val sePuedeEnviar: Boolean get() = emailValido && passwordValida && !enviando

    companion object {
        /** El minimo que exige Supabase por defecto. */
        const val MINIMO_PASSWORD = 6
    }
}

// Un `enum` por campo: cada caso es UN motivo. La pantalla elige el texto de
// cada uno (y lo traduce); las reglas no saben nada de textos.
enum class ErrorEmail { VACIO, NO_VALIDO }

enum class ErrorPassword { VACIA, CORTA }
