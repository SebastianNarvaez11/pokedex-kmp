package com.sebastiannarvaez.pokedex.android.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import com.sebastiannarvaez.pokedex.feature.search.SearchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

private fun pokemon(id: Int, nombre: String) = Pokemon(
    id = id,
    name = nombre,
    types = listOf(PokemonType.GRASS),
)

/** El estado de carga «ya terminó», que es lo que ve una lista cargada. */
private val TERMINADO = LoadStates(
    refresh = LoadState.NotLoading(false),
    prepend = LoadState.NotLoading(true),
    append = LoadState.NotLoading(true),
)

@RunWith(RobolectricTestRunner::class)
class PokemonListContentTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Pintar(
        datos: PagingData<Pokemon>,
        favoritos: Set<Int> = emptySet(),
        alPulsar: (Int) -> Unit = {},
        alMarcar: (Pokemon) -> Unit = {},
    ) {
        val flujo = remember { MutableStateFlow(datos) }
        PokemonListContent(
            pokemon = flujo.collectAsLazyPagingItems(),
            busqueda = SearchUiState(),
            expandida = false,
            alCambiarExpansion = {},
            alEscribir = {},
            alLimpiar = {},
            alPulsar = alPulsar,
            idsFavoritos = favoritos,
            alMarcar = alMarcar,
        )
    }

    @Test
    fun `la lista ensena los nombres que recibe`() {
        compose.setContent {
            Pintar(PagingData.from(listOf(pokemon(1, "bulbasaur"), pokemon(4, "charmander")), TERMINADO))
        }

        // La tarjeta pinta el nombre con la inicial en mayuscula: PokeAPI lo
        // devuelve todo en minusculas y la interfaz lo arregla. El test busca
        // lo que se **ve**, no lo que se guarda.
        compose.onNodeWithText("Bulbasaur").assertIsDisplayed()
        // El segundo cae fuera de la ventana del test. En una rejilla perezosa
        // eso no es «esta oculto»: es que **no existe**, porque Compose no lo
        // ha compuesto todavia. Hay que desplazar la rejilla hasta el.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Charmander"))
        compose.onNodeWithText("Charmander").assertIsDisplayed()
    }

    @Test
    fun `pulsar una tarjeta devuelve su identificador`() {
        var pulsado = -1
        compose.setContent {
            Pintar(PagingData.from(listOf(pokemon(25, "pikachu")), TERMINADO), alPulsar = { pulsado = it })
        }

        compose.onNodeWithText("Pikachu").performClick()

        assertEquals(25, pulsado)
    }

    /**
     * Lista vacia y lista cargando no son el mismo estado, y confundirlos deja
     * una ruleta girando para siempre cuando de verdad no hay nada.
     */
    @Test
    fun `sin resultados se explica, no se deja girando`() {
        compose.setContent { Pintar(PagingData.from(emptyList(), TERMINADO)) }

        compose.onNodeWithText("No hay nada que enseñar", substring = true).assertIsDisplayed()
    }

    @Test
    fun `un fallo de red ofrece reintentar`() {
        val conError = PagingData.from<Pokemon>(
            emptyList(),
            LoadStates(
                refresh = LoadState.Error(java.io.IOException("socket cerrado")),
                prepend = LoadState.NotLoading(true),
                append = LoadState.NotLoading(true),
            ),
        )

        compose.setContent { Pintar(conError) }

        compose.onNodeWithText("Reintentar").assertIsDisplayed()
    }
}
