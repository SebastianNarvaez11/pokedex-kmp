package com.sebastiannarvaez.pokedex

import kotlin.test.Test
import kotlin.test.assertTrue

class GreetingTest {

    @Test
    fun elSaludoNombraLaPlataforma() {
        // El mismo test corre en JVM, en Android y en el simulador de iOS, y en
        // cada uno el nombre es distinto. Por eso se comprueba la forma, no el texto.
        val saludo = Greeting().greet()
        assertTrue(saludo.startsWith("Hola desde "), "saludo inesperado: $saludo")
        assertTrue(saludo.length > "Hola desde ".length, "falta el nombre de la plataforma")
    }
}
