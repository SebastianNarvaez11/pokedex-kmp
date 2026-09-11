package com.sebastiannarvaez.pokedex.android.search

import com.sebastiannarvaez.pokedex.android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sebastiannarvaez.pokedex.domain.PokemonRef
import com.sebastiannarvaez.pokedex.feature.search.SearchUiState

/**
 * La barra de busqueda de Material 3.
 *
 * Se expande a pantalla completa con sus resultados dentro, y el propio
 * componente gestiona la animacion, el foco y el gesto de volver. En iOS esto
 * lo hace el sistema con un modificador; aqui es un componente que envuelve.
 *
 * Los resultados se pintan como **filas**, no como tarjetas: una lista larga se
 * recorre mejor en vertical, y ademas no tenemos los tipos de cada resultado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonSearchBar(
    estado: SearchUiState,
    expandida: Boolean,
    alCambiarExpansion: (Boolean) -> Unit,
    alEscribir: (String) -> Unit,
    alLimpiar: () -> Unit,
    alElegir: (PokemonRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        modifier = modifier,
        inputField = {
            SearchBarDefaults.InputField(
                query = estado.consulta,
                onQueryChange = alEscribir,
                onSearch = { alCambiarExpansion(true) },
                expanded = expandida,
                onExpandedChange = alCambiarExpansion,
                placeholder = { Text(stringResource(R.string.buscar_pokemon)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (estado.consulta.isNotEmpty()) {
                        IconButton(onClick = alLimpiar) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.borrar_busqueda))
                        }
                    }
                },
            )
        },
        expanded = expandida,
        onExpandedChange = alCambiarExpansion,
    ) {
        when {
            estado.enReposo -> Aviso(stringResource(R.string.busqueda_vacia))

            estado.buscando && estado.resultados.isEmpty() ->
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }

            estado.error != null -> Aviso(estado.error!!.detalle)

            estado.sinResultados -> Aviso(stringResource(R.string.busqueda_sin_resultados))

            else -> LazyColumn {
                items(estado.resultados, key = { it.id }) { ref ->
                    FilaDeResultado(ref, alPulsar = { alElegir(ref) })
                }
            }
        }
    }
}

@Composable
private fun FilaDeResultado(ref: PokemonRef, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alPulsar)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = ref.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                ref.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                stringResource(R.string.numero_pokemon, ref.id),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Aviso(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}
