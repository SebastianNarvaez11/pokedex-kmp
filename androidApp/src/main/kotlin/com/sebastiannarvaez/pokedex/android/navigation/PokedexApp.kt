package com.sebastiannarvaez.pokedex.android.navigation

import com.sebastiannarvaez.pokedex.android.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sebastiannarvaez.pokedex.navigation.Destination
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sebastiannarvaez.pokedex.android.favorites.FavoritesScreen
import com.sebastiannarvaez.pokedex.android.settings.SettingsScreen
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
fun PokedexApp(
    destinoEntrante: Destination? = null,
    alConsumirDestino: () -> Unit = {},
) {
    val pila = rememberNavBackStack(ListaKey)
    val actual = pila.lastOrNull()

    // Un enlace entrante deja la lista debajo y el destino encima: asi volver
    // desde una ficha abierta por enlace lleva a la app, no fuera de ella.
    LaunchedEffect(destinoEntrante) {
        val destino = destinoEntrante ?: return@LaunchedEffect
        val clave = destino.aNavKey()
        pila.clear()
        if (clave != ListaKey) pila.add(ListaKey)
        pila.add(clave)
        alConsumirDestino()
    }

    Scaffold(
        bottomBar = {
            // La barra solo aparece en las pantallas de primer nivel. En un
            // detalle estorba: ocupa sitio y ofrece saltar a otra seccion
            // justo cuando el usuario acaba de entrar en algo.
            AnimatedVisibility(
                visible = actual in PESTANAS,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = actual == ListaKey,
                        onClick = { irAPestana(pila, ListaKey) },
                        icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                        label = { Text(stringResource(R.string.pestana_pokedex)) },
                    )
                    NavigationBarItem(
                        selected = actual == FavoritosKey,
                        onClick = { irAPestana(pila, FavoritosKey) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text(stringResource(R.string.pestana_favoritos)) },
                    )
                    NavigationBarItem(
                        selected = actual == AjustesKey,
                        onClick = { irAPestana(pila, AjustesKey) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.pestana_ajustes)) },
                    )
                }
            }
        },
    ) { relleno ->
    NavDisplay(
        modifier = Modifier.padding(relleno),
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
            entry<FavoritosKey> {
                FavoritesScreen(alPulsar = { id -> pila.add(DetalleKey(id)) })
            }
            entry<AjustesKey> { SettingsScreen() }
            entry<DetalleKey> { clave ->
                PokemonDetailScreen(
                    pokemonId = clave.pokemonId,
                    alVolver = { if (pila.size > 1) pila.removeAt(pila.lastIndex) },
                )
            }
        },
    )
    }
}

/**
 * Cambiar de pestaña vacía la pila y deja la pestaña sola.
 *
 * Navigation 3 no trae pilas separadas por pestaña, y montarlas a mano es más
 * complejo de lo que parece. Con una sola pila, volver desde una pestaña
 * distinta de la inicial cierra la app; para evitarlo, la lista siempre queda
 * debajo.
 */
private fun irAPestana(pila: MutableList<NavKey>, destino: NavKey) {
    if (pila.lastOrNull() == destino) return
    pila.clear()
    if (destino != ListaKey) pila.add(ListaKey)
    pila.add(destino)
}
