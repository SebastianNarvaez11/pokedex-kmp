package com.sebastiannarvaez.pokedex.core

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter

/**
 * El registro de la app, con una sola etiqueta para todo.
 *
 * Se usa un `Logger` propio y no el estatico de Kermit porque el estatico es
 * estado global: dos modulos que lo configuren distinto se pisan, y en un test
 * no hay forma de silenciarlo.
 *
 * `platformLogWriter()` resuelve solo el destino de cada plataforma: Logcat en
 * Android y `os_log` en iOS. No hay que escribir nada por plataforma.
 */
object Log {

    private val logger: Logger = Logger(
        config = loggerConfigInit(
            platformLogWriter(),
            // En una compilacion de release este umbral sube, para no dejar en
            // el registro del dispositivo nada que no deba estar ahi.
            minSeverity = Severity.Debug,
        ),
        tag = "Pokedex",
    )

    fun d(mensaje: String) = logger.d(mensaje)

    fun i(mensaje: String) = logger.i(mensaje)

    fun w(mensaje: String, causa: Throwable? = null) = logger.w(causa) { mensaje }

    fun e(mensaje: String, causa: Throwable? = null) = logger.e(causa) { mensaje }
}
