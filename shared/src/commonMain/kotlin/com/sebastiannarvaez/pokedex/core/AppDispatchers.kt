package com.sebastiannarvaez.pokedex.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Los tres hilos de trabajo de la app, como dependencia y no como constante.
 *
 * Se inyecta en vez de llamar a `Dispatchers` directamente por una razon muy
 * concreta: en un test se sustituye por el dispatcher de prueba y `runTest`
 * puede adelantar el tiempo virtual. Con `Dispatchers.IO` escrito a pelo dentro
 * de un repositorio, ese test tiene que esperar de verdad.
 */
interface AppDispatchers {
    /** Trabajo que bloquea el hilo: disco, y en la JVM tambien la red. */
    val io: CoroutineDispatcher

    /** Trabajo que consume CPU: parsear, ordenar, transformar listas. */
    val default: CoroutineDispatcher

    /** El hilo de la interfaz. */
    val main: CoroutineDispatcher
}

internal class PlatformAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher = ioDispatcher()
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}

/**
 * `Dispatchers.IO` no se puede usar desde `commonMain`: existe, pero es
 * `internal` en la libreria. En Kotlin/Native tampoco esta disponible, asi que
 * iOS usa `Default`, que ahi tambien esta respaldado por un pool de hilos.
 */
internal expect fun ioDispatcher(): CoroutineDispatcher
