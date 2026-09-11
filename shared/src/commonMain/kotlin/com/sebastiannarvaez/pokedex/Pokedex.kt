package com.sebastiannarvaez.pokedex

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.core.PlatformAppDispatchers
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * La puerta de entrada del modulo compartido.
 *
 * Existe para que cada app tenga **un solo simbolo** que conocer. Sin ella, la
 * interfaz de iOS tendria que llamar a la funcion de nivel superior que resuelve
 * la plataforma, cuyo nombre en Swift sale del fichero donde vive el `actual`.
 *
 * Aqui dentro iran, segun avance el curso, el cliente de red, la base de datos
 * y los repositorios.
 */
class Pokedex internal constructor(
    internal val dispatchers: AppDispatchers,
) {

    /**
     * El constructor que usan las dos apps.
     *
     * Es un constructor secundario y no un argumento por defecto porque **los
     * argumentos por defecto no cruzan a Swift**: con `dispatchers` por defecto,
     * el header solo declara `init(dispatchers:)` y `Pokedex()` responde
     * «'init()' is unavailable».
     */
    constructor() : this(PlatformAppDispatchers())

    private val platform: Platform = currentPlatform()

    fun greeting(): String = Greeting(platform).greet()

    /**
     * Latido: un contador que emite cada segundo mientras alguien escuche.
     *
     * No hace nada util, y por eso esta: comprueba que un `Flow` de Kotlin
     * llega entero a las dos interfaces antes de que haya datos de verdad que
     * transportar. Desaparece cuando llegue la lista de Pokemon.
     *
     * Es un flujo **frio**: no cuenta nada hasta que alguien se suscribe, y se
     * para solo cuando el ultimo suscriptor se va.
     */
    fun heartbeat(): Flow<Int> = flow {
        var latido = 0
        while (true) {
            emit(latido++)
            delay(1.seconds)
        }
    }.flowOn(dispatchers.default)
}
