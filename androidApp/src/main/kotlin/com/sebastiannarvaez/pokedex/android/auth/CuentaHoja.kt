package com.sebastiannarvaez.pokedex.android.auth

import com.sebastiannarvaez.pokedex.android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebastiannarvaez.pokedex.feature.auth.AccountViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.pokedex.domain.Session

/**
 * La cuenta, en una hoja inferior.
 *
 * Cerrar sesion es una accion destructiva de las suaves: no borra nada, pero
 * saca al usuario de la app. Una hoja modal la pone delante sin cambiar de
 * pantalla y se cierra deslizando, que es lo que espera alguien que la abrio
 * por curiosidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentaHoja(
    session: Session,
    alCerrar: () -> Unit,
    alSalir: () -> Unit,
    cuenta: AccountViewModel = koinViewModel(),
) {
    var confirmandoBorrado by remember { mutableStateOf(false) }
    val estado by cuenta.state.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Column {
                    Text(
                        session.email ?: stringResource(R.string.auth_tu_cuenta),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.auth_sesion_iniciada),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                stringResource(R.string.auth_favoritos_locales),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(onClick = alSalir, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_cerrar_sesion))
            }

            // Eliminar la cuenta es obligatorio en las dos tiendas si la app
            // deja crearla. No es una cortesia: sin esta opcion, la revision la
            // rechaza.
            TextButton(
                onClick = { confirmandoBorrado = true },
                enabled = !estado.trabajando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auth_eliminar_cuenta), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = { confirmandoBorrado = false },
            title = { Text(stringResource(R.string.auth_eliminar_cuenta_titulo)) },
            text = {
                Text(
                    stringResource(R.string.auth_eliminar_cuenta_detalle),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoBorrado = false
                        cuenta.borrarCuenta()
                    },
                ) {
                    Text(stringResource(R.string.auth_eliminar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoBorrado = false }) { Text(stringResource(R.string.auth_cancelar)) }
            },
        )
    }
}
