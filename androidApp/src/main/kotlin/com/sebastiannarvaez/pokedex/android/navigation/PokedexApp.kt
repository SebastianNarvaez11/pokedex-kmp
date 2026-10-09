package com.sebastiannarvaez.pokedex.android.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
    // La pila es una lista de claves que sobrevive a rotar y a que maten el proceso.
    val pila = rememberNavBackStack(inicio)

    NavDisplay(
        backStack = pila,
        // «Atrás» quita la última clave, pero solo si queda otra debajo.
        onBack = { if (pila.size > 1) pila.removeAt(pila.lastIndex) },
        entryDecorators = listOf(
            // Primero el de rememberSaveable: la lista conserva el scroll al volver.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Y el de ViewModel: cada ficha tiene el suyo y se limpia al salir.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ListaKey> {
                // La lista no sabe adónde lleva un toque: avisa, y aquí se decide.
                PokemonListScreen(
                    alPulsar = { id -> pila.add(DetalleKey(id)) },
                )
            }
            entry<DetalleKey> { clave ->
                // clave es la DetalleKey de arriba de la pila, con su pokemonId tipado.
                PokemonDetailScreen(
                    pokemonId = clave.pokemonId,
                    alVolver = { if (pila.size > 1) pila.removeAt(pila.lastIndex) },
                )
            }
        },
    )
}
