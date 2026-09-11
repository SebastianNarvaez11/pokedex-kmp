package com.sebastiannarvaez.pokedex.android.auth

import com.sebastiannarvaez.pokedex.android.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.feature.auth.AuthFormState
import com.sebastiannarvaez.pokedex.feature.auth.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * La puerta de la app.
 *
 * Entrar y registrarse son **la misma pantalla** con un selector arriba. Son
 * dos campos iguales y un boton, y separarlas en dos destinos obligaria a
 * navegar de una a otra perdiendo lo escrito.
 *
 * El selector es `SingleChoiceSegmentedButtonRow`, que es lo que Material 3
 * ofrece para una eleccion entre pocas opciones excluyentes. iOS resuelve lo
 * mismo con un `Picker` de estilo segmentado: se parecen a la vista y se
 * escriben distinto, que es justo el motivo de no compartir la interfaz.
 */
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val formulario by viewModel.form.collectAsStateWithLifecycle()

    AuthContent(
        formulario = formulario,
        alEscribirEmail = viewModel::escribirEmail,
        alEscribirPassword = viewModel::escribirPassword,
        alEntrar = viewModel::entrar,
        alRegistrar = viewModel::registrar,
        alRecuperar = viewModel::recuperar,
        alDescartar = viewModel::descartarError,
        modifier = modifier,
    )
}

/**
 * La pantalla sin ViewModel.
 *
 * Separarla no es ceremonia: asi se puede pintar en una vista previa y, sobre
 * todo, probarla sin montar el grafo de dependencias entero. Un test que
 * necesita Koin, Room y tres clientes de Ktor para comprobar que un boton se
 * habilita no comprueba el boton: comprueba el arranque de la app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthContent(
    formulario: AuthFormState,
    alEscribirEmail: (String) -> Unit,
    alEscribirPassword: (String) -> Unit,
    alEntrar: () -> Unit,
    alRegistrar: () -> Unit,
    alRecuperar: () -> Unit,
    alDescartar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var registrando by remember { mutableStateOf(false) }
    val teclado = LocalSoftwareKeyboardController.current

    val enviar = {
        teclado?.hide()
        if (registrando) alRegistrar() else alEntrar()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // `imePadding` sube el contenido con el teclado. Sin el, el
                // boton queda debajo del teclado y no hay forma de pulsarlo en
                // pantallas pequenas.
                .imePadding()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                stringResource(R.string.pestana_pokedex),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.auth_bienvenida),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            // Las etiquetas de prueba existen porque el selector y el boton
            // dicen **lo mismo**: «Entrar» aparece dos veces en la pantalla, y
            // buscar por texto encuentra los dos. Sin ellas, el test falla con
            // «Expected exactly 1 node but found 2».
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !registrando,
                    onClick = { registrando = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag(TAG_MODO_ENTRAR),
                ) { Text(stringResource(R.string.auth_entrar)) }
                SegmentedButton(
                    selected = registrando,
                    onClick = { registrando = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag(TAG_MODO_REGISTRAR),
                ) { Text(stringResource(R.string.auth_crear_cuenta)) }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = formulario.email,
                onValueChange = alEscribirEmail,
                label = { Text(stringResource(R.string.auth_correo)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                // El teclado de correo trae la arroba a mano y quita la
                // mayuscula automatica, que es la causa numero uno de «mi
                // correo no existe».
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                isError = formulario.email.isNotBlank() && !formulario.emailValido,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            var verPassword by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = formulario.password,
                onValueChange = alEscribirPassword,
                label = { Text(stringResource(R.string.auth_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    TextButton(onClick = { verPassword = !verPassword }) {
                        Text(if (verPassword) stringResource(R.string.auth_password_ocultar) else stringResource(R.string.auth_password_ver))
                    }
                },
                visualTransformation = if (verPassword) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (formulario.sePuedeEnviar) enviar() }),
                supportingText = {
                    if (registrando) Text(stringResource(R.string.auth_password_minimo, AuthFormState.MINIMO_PASSWORD))
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // El error aparece y desaparece con animacion, en el sitio donde se
            // mira: justo encima del boton que se acaba de pulsar.
            AnimatedVisibility(visible = formulario.error != null) {
                formulario.error?.let { AvisoDeError(it) }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = enviar,
                enabled = formulario.sePuedeEnviar,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag(TAG_ENVIAR),
            ) {
                if (formulario.enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (registrando) stringResource(R.string.auth_crear_cuenta) else stringResource(R.string.auth_entrar))
                }
            }

            // Recuperar solo tiene sentido al entrar: quien se esta
            // registrando no tiene contrasena que olvidar.
            if (!registrando) {
                TextButton(
                    onClick = {
                        teclado?.hide()
                        alRecuperar()
                    },
                    enabled = formulario.emailValido && !formulario.enviando,
                    modifier = Modifier.padding(top = 8.dp).testTag(TAG_RECUPERAR),
                ) {
                    Text(stringResource(R.string.auth_olvidaste))
                }
                if (!formulario.emailValido) {
                    Text(
                        stringResource(R.string.auth_olvidaste_ayuda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }

    // El aviso no dice si el correo existe. Decirlo le contaria a cualquiera
    // quien tiene cuenta aqui, y Supabase responde igual en los dos casos.
    if (formulario.correoEnviado) {
        AlertDialog(
            onDismissRequest = alDescartar,
            confirmButton = {
                TextButton(onClick = alDescartar) { Text(stringResource(R.string.auth_entendido)) }
            },
            title = { Text(stringResource(R.string.auth_revisa_correo)) },
            text = {
                Text(
                    stringResource(R.string.auth_revisa_correo_detalle),
                )
            },
        )
    }
}

/** Las etiquetas que usan los tests, en un solo sitio y no sueltas por ahi. */
const val TAG_MODO_ENTRAR = "modo-entrar"
const val TAG_MODO_REGISTRAR = "modo-registrar"
const val TAG_ENVIAR = "enviar"
const val TAG_RECUPERAR = "recuperar"

@Composable
private fun AvisoDeError(error: UiError) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(error.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(error.detalle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Lo que se ve mientras se lee el token guardado. Dura un parpadeo. */
@Composable
fun ComprobandoSesion(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
