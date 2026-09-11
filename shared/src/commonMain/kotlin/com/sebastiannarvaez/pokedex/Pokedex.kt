package com.sebastiannarvaez.pokedex

/**
 * La puerta de entrada del modulo compartido.
 *
 * Existe para que cada app tenga **un solo simbolo** que conocer. Sin ella, la
 * interfaz de iOS tendria que llamar a `Platform_iosKt.currentPlatform()`, un
 * nombre que sale del fichero donde vive el `actual` y que se cuela en la API
 * publica de Swift sin que nada en el codigo comun lo insinue.
 *
 * Aqui dentro iran, segun avance el curso, el cliente de red, la base de datos
 * y los repositorios. De momento solo resuelve la plataforma.
 */
class Pokedex {

    private val platform: Platform = currentPlatform()

    fun greeting(): String = Greeting(platform).greet()
}
