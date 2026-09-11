package com.sebastiannarvaez.pokedex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Una plataforma de mentira. Tres lineas, sin libreria de mocks.
 *
 * Los mocks por reflexion no funcionan en Kotlin/Native, asi que en KMP los
 * dobles se escriben a mano. No es un apano: es lo que recomienda hacer.
 */
private class FakePlatform(override val name: String) : Platform

class GreetingTest {

    @Test
    fun elSaludoNombraLaPlataformaQueSeLePasa() {
        // Sin la interfaz, este test no se podria escribir: diria una cosa en
        // la JVM, otra en Android y otra en el simulador de iOS.
        val saludo = Greeting(FakePlatform("Plataforma de prueba")).greet()

        assertEquals("Hola desde Plataforma de prueba", saludo)
    }

    @Test
    fun cadaPlataformaRealSeIdentifica() {
        // Este si depende de donde corra, y por eso solo comprueba la forma.
        // Es el unico test del modulo que no puede afirmar un valor concreto.
        val saludo = Greeting(currentPlatform()).greet()

        assertTrue(saludo.startsWith("Hola desde "), "saludo inesperado: $saludo")
        assertTrue(saludo.length > "Hola desde ".length, "falta el nombre de la plataforma")
    }
}
