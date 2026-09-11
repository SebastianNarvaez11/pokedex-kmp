package com.sebastiannarvaez.pokedex.android.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.sebastiannarvaez.pokedex.android.detail.PokemonDetailScreen
import com.sebastiannarvaez.pokedex.android.list.PokemonListScreen

/**
 * La pila de pantallas.
 *
 * Navigation 3 no esconde la pila detras de un grafo: **es una lista**, se
 * mira y se modifica. Empujar una pantalla es anadir al final, y volver es
 * quitar el ultimo.
 */
@Composable
fun PokedexApp(inicio: NavKey = ListaKey) {
    val pila = rememberNavBackStack(inicio)

    NavDisplay(
        backStack = pila,
        onBack = { if (pila.size > 1) pila.removeAt(pila.lastIndex) },
        // Cada entrada de la pila con su propio almacen de ViewModel. Sin este
        // decorador todas comparten uno, y al abrir un Pokemon se ve la ficha
        // del anterior durante un instante.
        entryDecorators = listOf(rememberViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<ListaKey> {
                PokemonListScreen(
                    alPulsar = { id -> pila.add(DetalleKey(id)) },
                )
            }
            entry<DetalleKey> { clave ->
                PokemonDetailScreen(
                    pokemonId = clave.pokemonId,
                    alVolver = { if (pila.size > 1) pila.removeAt(pila.lastIndex) },
                )
            }
        },
    )
}
