package com.sebastiannarvaez.pokedex.android.auth

import com.sebastiannarvaez.pokedex.android.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebastiannarvaez.pokedex.feature.auth.AccountViewModel
import com.sebastiannarvaez.pokedex.feature.auth.AuthFormState
import org.koin.compose.viewmodel.koinViewModel

/**
 * La contrasena nueva, tras abrir el enlace del correo.
 *
 * Es un dialogo y no una pantalla porque el usuario **ya esta dentro**: el
 * enlace trae una sesion temporal. Sacarlo a una pantalla completa dejaria la
 * app detras en un estado raro, a medio camino entre fuera y dentro.
 */
@Composable
fun NuevaPasswordDialog(
    alTerminar: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    var password by remember { mutableStateOf("") }
    val estado by viewModel.state.collectAsStateWithLifecycle()

    if (estado.passwordCambiada) {
        AlertDialog(
            onDismissRequest = alTerminar,
            confirmButton = { TextButton(onClick = alTerminar) { Text(stringResource(R.string.auth_listo)) } },
            title = { Text(stringResource(R.string.auth_password_cambiada)) },
            text = { Text(stringResource(R.string.auth_password_cambiada_detalle)) },
        )
        return
    }

    AlertDialog(
        onDismissRequest = alTerminar,
        title = { Text(stringResource(R.string.auth_password_nueva)) },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.auth_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = { Text(stringResource(R.string.auth_password_minimo, AuthFormState.MINIMO_PASSWORD)) },
                    isError = estado.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                estado.error?.let {
                    Text(
                        it.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.cambiarPassword(password) },
                enabled = password.length >= AuthFormState.MINIMO_PASSWORD && !estado.trabajando,
            ) { Text(stringResource(R.string.auth_guardar)) }
        },
        dismissButton = { TextButton(onClick = alTerminar) { Text(stringResource(R.string.auth_ahora_no)) } },
    )
}
