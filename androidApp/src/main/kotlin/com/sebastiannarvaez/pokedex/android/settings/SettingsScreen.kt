package com.sebastiannarvaez.pokedex.android.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebastiannarvaez.pokedex.feature.settings.SettingsViewModel
import com.sebastiannarvaez.pokedex.feature.settings.Tema
import org.koin.compose.viewmodel.koinViewModel

/**
 * Los ajustes, con una hoja inferior para elegir el tema.
 *
 * La hoja modal es la forma de Android de pedir una eleccion corta sin sacar al
 * usuario de la pantalla. En iOS el equivalente natural es un `Picker` dentro
 * de un `Form`, que se ve y se usa distinto, y por eso se escribe aparte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val ajustes by viewModel.settings.collectAsStateWithLifecycle()
    var hojaTema by remember { mutableStateOf(false) }
    val estadoHoja = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Ajustes", fontWeight = FontWeight.Bold) }) },
    ) { relleno ->
        Column(modifier = Modifier.padding(relleno).fillMaxSize()) {
            Fila(
                titulo = "Tema",
                detalle = ajustes.tema.etiqueta,
                icono = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                alPulsar = { hojaTema = true },
            )
            Fila(
                titulo = "Ordenar favoritos por número",
                detalle = if (ajustes.favoritosPorNumero) "Del 1 al último" else "Lo más reciente primero",
                icono = {
                    Switch(
                        checked = ajustes.favoritosPorNumero,
                        onCheckedChange = viewModel::cambiarOrden,
                    )
                },
                alPulsar = { viewModel.cambiarOrden(!ajustes.favoritosPorNumero) },
            )
            Fila(
                titulo = "Acerca de",
                detalle = "Pokédex · datos de PokeAPI",
                icono = { Icon(Icons.Default.Info, contentDescription = null) },
                alPulsar = {},
            )
        }
    }

    if (hojaTema) {
        ModalBottomSheet(onDismissRequest = { hojaTema = false }, sheetState = estadoHoja) {
            Text(
                "Tema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Tema.entries.forEach { opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.cambiarTema(opcion)
                            hojaTema = false
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (opcion == ajustes.tema) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.size(20.dp),
                    )
                    Text(opcion.etiqueta)
                }
            }
            Text(
                "«Lo que diga el sistema» sigue el ajuste de Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun Fila(titulo: String, detalle: String, icono: @Composable () -> Unit, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alPulsar)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        icono()
    }
}

val Tema.etiqueta: String
    get() = when (this) {
        Tema.SISTEMA -> "Lo que diga el sistema"
        Tema.CLARO -> "Claro"
        Tema.OSCURO -> "Oscuro"
    }
