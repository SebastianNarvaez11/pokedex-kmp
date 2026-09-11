package com.sebastiannarvaez.pokedex.android.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sebastiannarvaez.pokedex.core.aAppError
import com.sebastiannarvaez.pokedex.core.aUiError
import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.feature.list.PokemonListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PokemonListScreen(
    modifier: Modifier = Modifier,
    viewModel: PokemonListViewModel = koinViewModel(),
) {
    // collectAsLazyPagingItems es el puente que Android ya trae hecho. En iOS
    // hubo que escribirlo a mano: es la misma pieza, aqui regalada.
    val pokemon = viewModel.pokemon.collectAsLazyPagingItems()
    PokemonListContent(pokemon = pokemon, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PokemonListContent(
    pokemon: LazyPagingItems<Pokemon>,
    modifier: Modifier = Modifier,
) {
    // La barra grande se encoge al hacer scroll: es un gesto de Android, no
    // una imitacion de iOS.
    val comportamiento = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val refrescando = pokemon.loadState.refresh is LoadState.Loading && pokemon.itemCount > 0

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(comportamiento.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Pokédex", fontWeight = FontWeight.Bold) },
                scrollBehavior = comportamiento,
            )
        },
    ) { relleno ->
        PullToRefreshBox(
            isRefreshing = refrescando,
            onRefresh = { pokemon.refresh() },
            modifier = Modifier.fillMaxSize().padding(relleno),
        ) {
            when {
                // Primera carga, sin nada que enseñar todavia.
                pokemon.loadState.refresh is LoadState.Loading && pokemon.itemCount == 0 ->
                    Centrado { CircularProgressIndicator() }

                pokemon.loadState.refresh is LoadState.Error && pokemon.itemCount == 0 -> {
                    val error = (pokemon.loadState.refresh as LoadState.Error).error.aAppError().aUiError()
                    Centrado {
                        EstadoDeError(
                            titulo = error.titulo,
                            detalle = error.detalle,
                            reintentar = if (error.sePuedeReintentar) ({ pokemon.retry() }) else null,
                        )
                    }
                }

                else -> Rejilla(pokemon)
            }
        }
    }
}

@Composable
private fun Rejilla(pokemon: LazyPagingItems<Pokemon>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 164.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = pokemon.itemCount,
            // `itemKey` y no una lambda propia. Escrita a mano, la tentacion es
            // `pokemon.peek(i)?.id ?: i`, y ahi esta la trampa: el identificador
            // 25 y el indice 25 son la misma clave. Con claves repetidas la
            // rejilla deja huecos en blanco, y parece que faltan datos cuando lo
            // que falta es una clave unica. `itemKey` resuelve el caso nulo sin
            // invadir el espacio de los identificadores.
            key = pokemon.itemKey { it.id },
            contentType = pokemon.itemContentType { "pokemon" },
        ) { indice ->
            pokemon[indice]?.let { PokemonCard(it) }
        }

        // El pie: cargando mas, o el error de ampliar con su reintento.
        when (val estado = pokemon.loadState.append) {
            is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                Centrado(alto = 72.dp) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
            }

            is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                val error = estado.error.aAppError().aUiError()
                EstadoDeError(
                    titulo = error.titulo,
                    detalle = error.detalle,
                    reintentar = if (error.sePuedeReintentar) ({ pokemon.retry() }) else null,
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun Centrado(alto: androidx.compose.ui.unit.Dp? = null, contenido: @Composable () -> Unit) {
    Box(
        modifier = if (alto != null) Modifier.fillMaxWidth().padding(vertical = 16.dp) else Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

@Composable
private fun EstadoDeError(titulo: String, detalle: String, reintentar: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (reintentar != null) {
            Button(onClick = reintentar, modifier = Modifier.padding(top = 8.dp)) { Text("Reintentar") }
        }
    }
}
