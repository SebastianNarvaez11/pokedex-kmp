package com.sebastiannarvaez.pokedex.android.detail

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sebastiannarvaez.pokedex.android.ui.color
import com.sebastiannarvaez.pokedex.android.ui.etiqueta
import com.sebastiannarvaez.pokedex.domain.PokemonDetail
import com.sebastiannarvaez.pokedex.domain.PokemonStat
import com.sebastiannarvaez.pokedex.domain.StatKind
import com.sebastiannarvaez.pokedex.feature.detail.PokemonDetailViewModel
import com.sebastiannarvaez.pokedex.navigation.Destination
import com.sebastiannarvaez.pokedex.navigation.toShareUrl
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PokemonDetailScreen(
    pokemonId: Int,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    // El identificador no sale del grafo, lo trae la navegacion: por eso se
    // pasa como parametro a Koin en vez de inyectarlo.
    viewModel: PokemonDetailViewModel = koinViewModel { parametersOf(pokemonId) },
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize()) {
        when {
            estado.cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

            estado.error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(estado.error!!.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    estado.error!!.detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (estado.error!!.sePuedeReintentar) {
                    Button(onClick = viewModel::reintentar) { Text("Reintentar") }
                }
            }

            estado.detalle != null -> Ficha(estado.detalle!!, alVolver)
        }
    }
}

@Composable
private fun Ficha(detalle: PokemonDetail, alVolver: () -> Unit) {
    val tono = detalle.types.firstOrNull()?.color ?: MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Brush.verticalGradient(listOf(tono.copy(alpha = 0.45f), Color.Transparent))),
        ) {
            // statusBarsPadding y no un relleno fijo: con enableEdgeToEdge el
            // contenido se pinta debajo de la barra de estado, y la altura de
            // esa barra cambia con el dispositivo. Sin esto, el boton de volver
            // queda encima del reloj.
            FilledTonalIconButton(
                onClick = alVolver,
                modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopStart),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }

            // Compartir usa el selector del sistema: no se decide por el
            // usuario a que app va. Se comparte el enlace `https` y no el
            // `pokedex://`, porque los esquemas propios no se convierten en
            // enlace pulsable en la mayoria de apps de mensajeria.
            val contexto = LocalContext.current
            FilledTonalIconButton(
                onClick = {
                    val enlace = Destination.Detalle(detalle.id).toShareUrl()
                    val texto = "${detalle.name.replaceFirstChar { it.uppercase() }} · $enlace"
                    contexto.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, texto)
                            },
                            null,
                        ),
                    )
                },
                modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopEnd),
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir")
            }

            Text(
                text = "N.º ${detalle.id.toString().padStart(4, '0')}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.statusBarsPadding().padding(top = 26.dp, end = 68.dp).align(Alignment.TopEnd),
            )

            AsyncImage(
                model = detalle.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(48.dp),
            )
        }

        Column(
            // navigationBarsPadding: la barra de gestos tambien se solapa, y el
            // ultimo dato quedaria medio tapado.
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        detalle.name.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (detalle.isLegendary) {
                        Text(
                            "Legendario",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                if (detalle.genus.isNotBlank()) {
                    Text(
                        detalle.genus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detalle.types.forEach { tipo ->
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tipo.color.copy(alpha = 0.20f))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(tipo.color))
                        Text(tipo.etiqueta, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (detalle.description.isNotBlank()) {
                Text(detalle.description, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Medida("Altura", "${detalle.heightCm / 100.0} m", Modifier.weight(1f))
                Medida("Peso", "${detalle.weightG / 1000.0} kg", Modifier.weight(1f))
            }

            if (detalle.stats.isNotEmpty()) {
                Text("Estadísticas base", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                detalle.stats.forEach { stat -> Estadistica(stat, tono) }
            }
        }
    }
}

@Composable
private fun Medida(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Estadistica(stat: PokemonStat, tono: Color) {
    val proporcion by animateFloatAsState(
        targetValue = stat.value.toFloat() / PokemonStat.MAXIMO,
        label = "estadistica",
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stat.kind.etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(width = 96.dp, height = 20.dp),
        )
        Text(
            stat.value.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.size(width = 34.dp, height = 20.dp),
        )
        LinearProgressIndicator(
            progress = { proporcion },
            color = tono,
            trackColor = tono.copy(alpha = 0.15f),
            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
        )
    }
}

private val StatKind.etiqueta: String
    get() = when (this) {
        StatKind.HP -> "PS"
        StatKind.ATTACK -> "Ataque"
        StatKind.DEFENSE -> "Defensa"
        StatKind.SPECIAL_ATTACK -> "At. especial"
        StatKind.SPECIAL_DEFENSE -> "Def. especial"
        StatKind.SPEED -> "Velocidad"
    }
