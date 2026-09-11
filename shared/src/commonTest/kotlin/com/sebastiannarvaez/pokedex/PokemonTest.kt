package com.sebastiannarvaez.pokedex

import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PokemonTest {

    @Test
    fun laIlustracionSeConstruyeDesdeElIdentificador() {
        val pikachu = Pokemon(id = 25, name = "pikachu", types = listOf(PokemonType.ELECTRIC))

        assertEquals(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png",
            pikachu.artworkUrl,
        )
    }

    @Test
    fun elTipoSeLeeSinImportarMayusculas() {
        assertEquals(PokemonType.GRASS, PokemonType.deApi("grass"))
        assertEquals(PokemonType.GRASS, PokemonType.deApi("GRASS"))
    }

    @Test
    fun unTipoDesconocidoNoRevientaLaApp() {
        // Si PokeAPI anade un tipo manana, la app publicada tiene que seguir
        // funcionando. Por eso devuelve null en vez de lanzar.
        assertNull(PokemonType.deApi("cosmico"))
    }
}
