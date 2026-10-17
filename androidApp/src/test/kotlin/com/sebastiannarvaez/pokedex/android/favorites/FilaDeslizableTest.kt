package com.sebastiannarvaez.pokedex.android.favorites

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import com.sebastiannarvaez.pokedex.domain.FavoritePokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * La accion «Quitar de favoritos» de TalkBack, comprobada sin TalkBack.
 *
 * Los gestos del menu de TalkBack no se pueden hacer con el raton en el
 * emulador; un test si puede pedir la accion por su nombre, como la pide el
 * lector de pantalla, al elemento que este enfoca.
 */
@RunWith(RobolectricTestRunner::class)
class FilaDeslizableTest {

    @get:Rule
    val compose = createComposeRule()

    // `performCustomAccessibilityActionWithLabel` es todavia experimental.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `la fila ofrece quitar de favoritos sin deslizar`() {
        var quitados = 0
        compose.setContent {
            FilaDeslizable(
                favorito = FavoritePokemon(1, "bulbasaur", PokemonType.GRASS),
                alPulsar = {},
                alQuitar = { quitados++ },
            )
        }

        // El elemento que enfoca TalkBack es la fila que se pulsa, la que tiene
        // el nombre. Si la accion estuviera en la caja de fuera, este nodo no la
        // tendria y el test fallaria con «Key not present: … CustomActions».
        compose.onNodeWithText("Bulbasaur").performCustomAccessibilityActionWithLabel("Quitar de favoritos")

        assertEquals(1, quitados)
    }
}
