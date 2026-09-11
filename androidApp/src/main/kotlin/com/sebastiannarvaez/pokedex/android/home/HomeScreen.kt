package com.sebastiannarvaez.pokedex.android.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.sebastiannarvaez.pokedex.feature.home.HomeUiState
import com.sebastiannarvaez.pokedex.feature.home.HomeViewModel

/**
 * La pantalla se parte en dos a proposito.
 *
 * `HomeScreen` habla con el ViewModel y no se puede previsualizar. `HomeContent`
 * solo recibe estado, asi que el IDE la pinta sin ejecutar nada del modulo
 * compartido. Es la separacion que hace que las vistas previas sirvan.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = koinViewModel()) {
    // collectAsStateWithLifecycle y no collectAsState: deja de escuchar cuando
    // la pantalla no se ve, y con el flujo frio de debajo eso apaga el trabajo.
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(estado = estado, modifier = modifier)
}

@Composable
private fun HomeContent(estado: HomeUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = estado.greeting, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Latido ${estado.heartbeat}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    MaterialTheme {
        HomeContent(estado = HomeUiState(greeting = "Hola desde Android 35", heartbeat = 7))
    }
}
