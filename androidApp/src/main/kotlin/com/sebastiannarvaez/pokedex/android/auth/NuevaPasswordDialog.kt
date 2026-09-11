package com.sebastiannarvaez.pokedex.android.auth

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
            confirmButton = { TextButton(onClick = alTerminar) { Text("Listo") } },
            title = { Text("Contraseña cambiada") },
            text = { Text("Ya puedes seguir usando la app con la contraseña nueva.") },
        )
        return
    }

    AlertDialog(
        onDismissRequest = alTerminar,
        title = { Text("Nueva contraseña") },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = { Text("Al menos ${AuthFormState.MINIMO_PASSWORD} caracteres") },
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
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alTerminar) { Text("Ahora no") } },
    )
}
