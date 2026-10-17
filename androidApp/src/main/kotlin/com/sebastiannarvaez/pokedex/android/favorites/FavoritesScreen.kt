package com.sebastiannarvaez.pokedex.android.favorites

import com.sebastiannarvaez.pokedex.android.R
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
import com.sebastiannarvaez.pokedex.android.ui.etiquetaRes
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
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
    val avisos = remember { SnackbarHostState() }   // quien enseña los Snackbar de esta pantalla

    // El aviso es un mensaje de una sola vez que vive en el estado: se enseña
    // mientras `quitado` no sea null y, al terminar, se le dice al ViewModel
    // que ya se vio. Si la pantalla se recrea a mitad (al girar el movil), se
    // vuelve a enseñar: no se pierde.
    // Los textos se resuelven fuera del efecto: `stringResource` es composable
    // y el bloque de `LaunchedEffect` no.
    val quitado = estado.quitado
    val mensaje = quitado?.let {
        stringResource(R.string.favorito_quitado, it.name.replaceFirstChar { c -> c.uppercase() })
    }
    val deshacer = stringResource(R.string.deshacer)
    LaunchedEffect(quitado) {
        if (quitado == null || mensaje == null) return@LaunchedEffect
        val resultado = avisos.showSnackbar(
            message = mensaje,
            actionLabel = deshacer,
            // Con un boton, el valor por defecto es Indefinite: no se iria nunca.
            duration = SnackbarDuration.Short,
        )
        if (resultado == SnackbarResult.ActionPerformed) viewModel.deshacer() else viewModel.mensajeMostrado()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.pestana_favoritos), fontWeight = FontWeight.Bold)
                        if (estado.favoritos.isNotEmpty()) {
                            // `pluralStringResource` y no un `if (n == 1)`: la
                            // regla del plural es del idioma, no del programa.
                            // El numero va **dos veces** a proposito: una para
                            // elegir la forma y otra para sustituir el %d. Es
                            // la trampa clasica de esta funcion, y olvidar la
                            // segunda deja un «%d favoritos» en pantalla.
                            Text(
                                pluralStringResource(
                                    R.plurals.favoritos_contados,
                                    estado.favoritos.size,
                                    estado.favoritos.size,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (estado.favoritos.isNotEmpty()) {
                        IconButton(onClick = { hojaAbierta = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.mas_opciones))
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
                        alQuitar = { viewModel.quitar(favorito) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
        }
    }

    if (hojaAbierta) {
        ModalBottomSheet(onDismissRequest = { hojaAbierta = false }, sheetState = estadoHoja) {
            Text(
                stringResource(R.string.pestana_favoritos),
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
                Text(stringResource(R.string.favoritos_quitar_todos), color = MaterialTheme.colorScheme.error)
            }
            Text(
                stringResource(R.string.favoritos_solo_local),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            )
        }
    }
}

// `internal` y no `private`: FilaDeslizableTest la pinta sola, sin la pantalla.
@Composable
internal fun FilaDeslizable(favorito: FavoritePokemon, alPulsar: () -> Unit, alQuitar: () -> Unit) {
    val umbral = SwipeToDismissBoxDefaults.positionalThreshold
    // remember y no rememberSwipeToDismissBoxState (que lo guarda con
    // rememberSaveable): si la fila vuelve con «Deshacer», tiene que volver
    // entera, no deslizada fuera de la pantalla como la dejo el gesto.
    val estadoDeslizamiento = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, umbral) }

    // La etiqueta se resuelve **antes** del bloque: `semantics { }` no es
    // composable, y `stringResource` si. Es el mismo motivo por el que las
    // etiquetas de tipo devuelven un identificador y no un texto.
    val etiquetaQuitar = stringResource(R.string.quitar_de_favoritos)

    SwipeToDismissBox(
        state = estadoDeslizamiento,
        enableDismissFromStartToEnd = false,   // solo hacia la izquierda
        // Se llama cuando la fila ya salio de la pantalla. `quitar` borra por
        // numero: si llegara dos veces, la segunda no encuentra nada.
        onDismiss = { alQuitar() },
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
        // Una accion que solo existe como gesto **no existe** para quien usa
        // TalkBack: el lector de pantalla se traga el deslizamiento. Declararla
        // como accion personalizada la pone en el menu de acciones, que es
        // donde TalkBack la busca. Va en la fila que TalkBack enfoca (la que se
        // pulsa), no en la caja de fuera: TalkBack solo ofrece las acciones
        // del elemento enfocado. iOS hace esto solo con `.swipeActions`.
        Fila(
            favorito,
            alPulsar,
            modifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction(etiquetaQuitar) {
                        alQuitar()
                        true   // `true`: la accion se hizo
                    },
                )
            },
        )
    }
}

@Composable
private fun Fila(favorito: FavoritePokemon, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = alPulsar)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = favorito.artworkUrl,
            // `null` a proposito, y no un descuido: la ilustracion **repite** el
            // nombre que hay justo al lado. Describirla haria que el lector de
            // pantalla dijera «Bulbasaur, imagen de Bulbasaur». Una imagen
            // decorativa se marca como decorativa.
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
                    stringResource(R.string.numero_pokemon, favorito.id),
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
                            stringResource(tipo.etiquetaRes),
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
        Text(stringResource(R.string.favoritos_vacio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.favoritos_vacio_detalle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
