package com.sebastiannarvaez.pokedex

import com.sebastiannarvaez.pokedex.core.Log

/**
 * La prueba de humo del proyecto: si esta frase aparece en las dos pantallas,
 * el codigo compartido esta llegando a Android y a iOS.
 */
class Greeting {
    private val platform = currentPlatform()

    fun greet(): String {
        val saludo = "Hola desde ${platform.name}"
        // La misma linea de codigo acaba en Logcat y en la consola de Xcode.
        Log.i(saludo)
        return saludo
    }
}
