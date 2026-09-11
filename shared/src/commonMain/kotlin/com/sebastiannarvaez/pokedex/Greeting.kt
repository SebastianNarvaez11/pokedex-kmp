package com.sebastiannarvaez.pokedex

import com.sebastiannarvaez.pokedex.core.Log

/**
 * La prueba de humo del proyecto: si esta frase aparece en las dos pantallas,
 * el codigo compartido esta llegando a Android y a iOS.
 *
 * Recibe la `Platform` en vez de pedirsela a `currentPlatform()`. Es la
 * diferencia entre una clase que se puede probar y una que no: en un test se le
 * pasa una implementacion falsa y el resultado deja de depender de donde corra.
 */
class Greeting(private val platform: Platform) {

    fun greet(): String {
        val saludo = "Hola desde ${platform.name}"
        // La misma linea de codigo acaba en Logcat y en la consola de Xcode.
        Log.i(saludo)
        return saludo
    }
}
