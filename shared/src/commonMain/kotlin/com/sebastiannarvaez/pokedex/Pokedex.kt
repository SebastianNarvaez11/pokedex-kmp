package com.sebastiannarvaez.pokedex

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.core.PlatformAppDispatchers

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
}
