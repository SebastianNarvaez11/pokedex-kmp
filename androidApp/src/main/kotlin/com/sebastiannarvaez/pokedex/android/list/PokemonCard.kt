package com.sebastiannarvaez.pokedex.android.list

import com.sebastiannarvaez.pokedex.android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sebastiannarvaez.pokedex.android.ui.color
import com.sebastiannarvaez.pokedex.android.ui.etiquetaRes
import com.sebastiannarvaez.pokedex.domain.Pokemon

/**
 * La tarjeta de la rejilla.
 *
 * El degradado sale del tipo principal: da identidad a cada tarjeta sin
 * necesidad de una imagen de fondo, y funciona igual en claro y en oscuro
 * porque el color se mezcla con la superficie del tema.
 */
@Composable
fun PokemonCard(
    pokemon: Pokemon,
    esFavorito: Boolean,
    alPulsar: () -> Unit,
    alMarcar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tono = pokemon.types.firstOrNull()?.color ?: MaterialTheme.colorScheme.primary

    // Se arma antes del bloque `semantics`, que no es composable. El
    // separador y el orden de las palabras salen de los recursos: « y » no se
    // traduce solo, y en ingles el tipo va **delante** de la palabra «type».
    val nombre = pokemon.name.replaceFirstChar { it.uppercase() }
    val separador = stringResource(R.string.separador_tipos)
    // Los textos se resuelven **fuera** de `joinToString`: su lambda no es
    // `inline`, asi que dentro no se puede llamar a nada `@Composable`. El
    // compilador lo dice con «@Composable invocations can only happen from the
    // context of a @Composable function», y el sitio que senala despista.
    val etiquetas = pokemon.types.map { stringResource(it.etiquetaRes) }
    val descripcion = if (etiquetas.isEmpty()) {
        nombre
    } else {
        stringResource(R.string.tarjeta_descripcion, nombre, etiquetas.joinToString(separador))
    }

    Card(
        onClick = alPulsar,
        modifier = modifier
            .fillMaxWidth()
            // Descripcion unica para el lector de pantalla, que si no leeria
            // el numero y el nombre como dos cosas sueltas.
            .semantics { contentDescription = descripcion },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .background(
                        Brush.verticalGradient(
                            listOf(tono.copy(alpha = 0.38f), tono.copy(alpha = 0.10f)),
                        ),
                    ),
            ) {
                Text(
                    text = stringResource(R.string.numero_pokemon, pokemon.id),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
                // El corazon va sobre la imagen y con su propia zona tactil:
                // marcar no debe abrir la ficha.
                IconButton(
                    onClick = alMarcar,
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp),
                ) {
                    Icon(
                        imageVector = if (esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (esFavorito) stringResource(R.string.quitar_de_favoritos) else stringResource(R.string.anadir_a_favoritos),
                        tint = if (esFavorito) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                AsyncImage(
                    model = pokemon.artworkUrl,
                    // null: la tarjeta entera ya tiene descripcion, y repetirla
                    // haria que el lector de pantalla la dijera dos veces.
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                )
            }

            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = pokemon.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // `FlowRow` y no `Row`: con la letra del sistema al 180 %, dos
                // etiquetas no caben en el ancho de la tarjeta y la segunda se
                // partia por la mitad —«Ven / eno»—. Asi baja a la linea
                // siguiente entera. Se descubrio poniendo la escala de fuente
                // al maximo en el emulador, no leyendo el codigo.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    pokemon.types.forEach { tipo -> TypeChip(stringResource(tipo.etiquetaRes), tipo.color) }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(texto: String, tono: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(tono.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(tono))
        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Una etiqueta de una palabra no se parte nunca: si no cabe, se
            // recorta con puntos suspensivos, que se entiende; partida, no.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
