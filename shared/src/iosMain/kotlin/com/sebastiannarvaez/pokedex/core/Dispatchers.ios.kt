package com.sebastiannarvaez.pokedex.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Kotlin/Native no expone `Dispatchers.IO`. `Default` esta respaldado por un
 * pool de hilos, que es lo que hace falta aqui: en iOS la red la resuelve
 * Darwin de forma asincrona, asi que no hay llamada bloqueante que aislar.
 */
internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default
