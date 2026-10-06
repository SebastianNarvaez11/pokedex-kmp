package com.sebastiannarvaez.pokedex.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * En iOS se usa `Default`, respaldado por un pool de hilos: la red la resuelve
 * Darwin de forma asincrona, asi que no hay llamada bloqueante que aislar.
 */
internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default
