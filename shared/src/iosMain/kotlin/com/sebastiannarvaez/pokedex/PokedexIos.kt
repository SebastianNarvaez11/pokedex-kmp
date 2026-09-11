package com.sebastiannarvaez.pokedex

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

/**
 * Los `Flow` que Swift puede recorrer.
 *
 * Las anotaciones viven aqui y no sobre la API comun a proposito: son cosa de
 * iOS, y no tienen por que ensuciar el codigo que comparten las dos apps.
 *
 * Kotlin/Native exporta un `Flow` como un tipo opaco que Swift no sabe
 * recorrer. El plugin genera, a partir de esta anotacion, un envoltorio que
 * `KMPNativeCoroutinesAsync` convierte en `AsyncSequence`.
 */
@NativeCoroutines
fun Pokedex.heartbeatForIos(): Flow<Int> = heartbeat()
