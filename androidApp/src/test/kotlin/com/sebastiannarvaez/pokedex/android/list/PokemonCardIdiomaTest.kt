package com.sebastiannarvaez.pokedex.android.list

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PokemonCardIdiomaTest {

    @get:Rule
    val compose = createComposeRule()

    private fun pintarBulbasaur() = compose.setContent {
        PokemonCard(
            pokemon = Pokemon(id = 1, name = "bulbasaur", types = listOf(PokemonType.GRASS, PokemonType.POISON)),
            esFavorito = false,
            alPulsar = {},
            alMarcar = {},
        )
    }

    // Sin anotación vale `qualifiers=es` del fichero de propiedades.
    @Test
    fun `en espanol el tipo va detras de la palabra tipo`() {
        pintarBulbasaur()
        // Para ver qué hay de verdad en pantalla, imprime el árbol de semántica.
        println(compose.onRoot().printToString())

        compose.onNodeWithContentDescription("Bulbasaur, tipo Planta y Veneno").assertExists()
    }

    // La anotación manda sobre el fichero, solo para este test.
    @Config(qualifiers = "en")
    @Test
    fun `en ingles el tipo va delante de la palabra type`() {
        pintarBulbasaur()

        compose.onNodeWithContentDescription("Bulbasaur, Grass and Poison type").assertExists()
    }
}
