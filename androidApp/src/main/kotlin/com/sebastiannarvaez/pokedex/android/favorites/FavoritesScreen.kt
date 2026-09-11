package com.sebastiannarvaez.pokedex.android.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sebastiannarvaez.pokedex.android.ui.color
import com.sebastiannarvaez.pokedex.android.ui.etiqueta
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sebastiannarvaez.pokedex.domain.FavoritePokemon
import com.sebastiannarvaez.pokedex.feature.favorites.FavoritesViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Los favoritos, con dos gestos que son de Android.
 *
 * Deslizar una fila para quitarla (`SwipeToDismissBox`) y una hoja inferior
 * modal para la accion destructiva. En iOS los equivalentes son otros —el
 * deslizamiento con acciones de `List` y una hoja con puntos de anclaje— y por
 * eso se escriben aparte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    alPulsar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var hojaAbierta by remember { mutableStateOf(false) }
    val estadoHoja = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Favoritos", fontWeight = FontWeight.Bold) },
                actions = {
                    if (estado.favoritos.isNotEmpty()) {
                        IconButton(onClick = { hojaAbierta = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                    }
                },
            )
        },
    ) { relleno ->
        when {
            estado.vacia -> Vacio(Modifier.padding(relleno))

            else -> LazyColumn(modifier = Modifier.padding(relleno).fillMaxSize()) {
                items(estado.favoritos, key = { it.id }) { favorito ->
                    FilaDeslizable(
                        favorito = favorito,
                        alPulsar = { alPulsar(favorito.id) },
                        alQuitar = { viewModel.toggle(favorito.id, favorito.name, favorito.primaryType) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
        }
    }

    if (hojaAbierta) {
        ModalBottomSheet(onDismissRequest = { hojaAbierta = false }, sheetState = estadoHoja) {
            Text(
                "Favoritos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.vaciar()
                        hojaAbierta = false
                    }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Quitar todos", color = MaterialTheme.colorScheme.error)
            }
            Text(
                "Los favoritos se guardan solo en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaDeslizable(favorito: FavoritePokemon, alPulsar: () -> Unit, alQuitar: () -> Unit) {
    val estadoDeslizamiento = rememberSwipeToDismissBoxState(
        // confirmValueChange y no un efecto posterior: asi la fila solo
        // desaparece si de verdad se quita, y no se queda un hueco si falla.
        confirmValueChange = { valor ->
            if (valor == SwipeToDismissBoxValue.EndToStart) {
                alQuitar()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = estadoDeslizamiento,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        Fila(favorito, alPulsar)
    }
}

@Composable
private fun Fila(favorito: FavoritePokemon, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = alPulsar)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = favorito.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                favorito.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "N.º ${favorito.id.toString().padStart(4, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // El tipo se guardo con el favorito, asi que esta fila se pinta
                // entera sin red. Los guardados antes de la version 2 no lo
                // tienen, y por eso puede ser nulo.
                favorito.primaryType?.let { tipo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(tipo.color))
                        Text(
                            tipo.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Vacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp),
        )
        Text("Todavía no hay favoritos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Toca el corazón de cualquier Pokémon para guardarlo aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
