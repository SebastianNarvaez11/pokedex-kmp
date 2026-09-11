package com.sebastiannarvaez.pokedex.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DestinationTest {

    @Test
    fun elEsquemaPropioAbreElDetalle() {
        assertEquals(Destination.Detalle(25), Destination.parse("pokedex://pokemon/25"))
    }

    @Test
    fun elEnlaceHttpsAbreLaMismaPantalla() {
        // Las dos formas llevan al mismo sitio: una sola traduccion, no dos.
        assertEquals(
            Destination.parse("pokedex://pokemon/25"),
            Destination.parse("https://pokedex-kmp.example/pokemon/25"),
        )
    }

    @Test
    fun laRaizEsLaLista() {
        assertEquals(Destination.Lista, Destination.parse("pokedex://"))
        assertEquals(Destination.Lista, Destination.parse("https://pokedex-kmp.example/"))
    }

    @Test
    fun loQueNoEntendemosNoRevienta() {
        // Alguien pegando cualquier cosa no es un error de programacion.
        assertNull(Destination.parse("pokedex://vaya/cosa"))
        assertNull(Destination.parse("pokedex://pokemon/pikachu"))
        assertNull(Destination.parse("no es una url"))
    }

    @Test
    fun todaPantallaSobreviveAlViajeDeIdaYVuelta() {
        // Una instancia de cada pantalla. La lista se mantiene a mano, y el
        // compilador ayuda por el otro lado: `toPath()` es un `when` exhaustivo
        // sobre una jerarquia sellada, asi que anadir una pantalla **no
        // compila** hasta traducirla. Este test cubre lo que el compilador no
        // ve: que `parse` sepa deshacer lo que `toPath` hizo.
        val todas: List<Destination> = listOf(
            Destination.Lista,
            Destination.Detalle(151),
        )

        todas.forEach { destino ->
            assertEquals(destino, Destination.parse(destino.toDeepLink()), "falla el esquema propio")
            assertEquals(destino, Destination.parse(destino.toShareUrl()), "falla el enlace https")
        }
    }
}
