package com.sebastiannarvaez.pokedex

/**
 * La prueba de humo del proyecto: si esta frase aparece en las dos pantallas,
 * el codigo compartido esta llegando a Android y a iOS.
 */
class Greeting {
    private val platform = currentPlatform()

    fun greet(): String = "Hola desde ${platform.name}"
}
