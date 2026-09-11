package com.sebastiannarvaez.pokedex.android.auth

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.feature.auth.AuthFormState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * Los tests de interfaz de Android, corriendo en la JVM.
 *
 * `RobolectricTestRunner` levanta un Android de mentira dentro del proceso de
 * la JVM. Eso los hace baratos —corren en el job de Linux, que cuesta la decima
 * parte que el de macOS— y rapidos. Lo que **no** cubren es el renderizado real
 * del sistema ni los gestos: para eso hace falta un dispositivo.
 */
@RunWith(RobolectricTestRunner::class)
class AuthContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun pintar(
        formulario: AuthFormState = AuthFormState(),
        alEscribirEmail: (String) -> Unit = {},
        alEscribirPassword: (String) -> Unit = {},
        alEntrar: () -> Unit = {},
        alRegistrar: () -> Unit = {},
        alRecuperar: () -> Unit = {},
    ) {
        compose.setContent {
            AuthContent(
                formulario = formulario,
                alEscribirEmail = alEscribirEmail,
                alEscribirPassword = alEscribirPassword,
                alEntrar = alEntrar,
                alRegistrar = alRegistrar,
                alRecuperar = alRecuperar,
                alDescartar = {},
            )
        }
    }

    @Test
    fun `con el formulario vacio no se puede entrar`() {
        pintar()

        compose.onNodeWithTag(TAG_ENVIAR).assertIsNotEnabled()
    }

    @Test
    fun `con correo y contrasena validos el boton se habilita`() {
        pintar(AuthFormState(email = "ash@pueblo-paleta.test", password = "pikachu"))

        compose.onNodeWithTag(TAG_ENVIAR).assertIsEnabled()
    }

    @Test
    fun `escribir en el campo de correo avisa al ViewModel`() {
        var escrito = ""
        pintar(alEscribirEmail = { escrito = it })

        compose.onNodeWithText("Correo").performTextInput("ash")

        assertEquals("ash", escrito)
    }

    @Test
    fun `pulsar entrar dispara la entrada y no el registro`() {
        var entradas = 0
        var registros = 0
        pintar(
            formulario = AuthFormState(email = "ash@pueblo-paleta.test", password = "pikachu"),
            alEntrar = { entradas++ },
            alRegistrar = { registros++ },
        )

        compose.onNodeWithTag(TAG_ENVIAR).performScrollTo().performClick()

        assertEquals(1, entradas)
        assertEquals(0, registros)
    }

    /**
     * El selector no es decorativo: cambia a que llamada va el mismo boton. Si
     * se rompiera, «Crear cuenta» intentaria entrar con una cuenta que no
     * existe, y el usuario veria «credenciales invalidas» al registrarse.
     */
    @Test
    fun `tras elegir crear cuenta el boton registra`() {
        var entradas = 0
        var registros = 0
        pintar(
            formulario = AuthFormState(email = "ash@pueblo-paleta.test", password = "pikachu"),
            alEntrar = { entradas++ },
            alRegistrar = { registros++ },
        )

        compose.onNodeWithTag(TAG_MODO_REGISTRAR).performScrollTo().performClick()
        compose.onNodeWithTag(TAG_ENVIAR).performScrollTo().performClick()

        assertEquals(0, entradas)
        assertEquals(1, registros)
    }

    @Test
    fun `el error se pinta con su titulo y su detalle`() {
        pintar(
            AuthFormState(
                email = "ash@pueblo-paleta.test",
                password = "pikachu",
                error = UiError("No se pudo continuar", "Invalid login credentials", false),
            ),
        )

        compose.onNodeWithText("No se pudo continuar").assertExists()
        compose.onNodeWithText("Invalid login credentials").assertExists()
    }

    @Test
    fun `sin correo valido no se puede pedir la recuperacion`() {
        pintar(AuthFormState(email = "ash"))

        compose.onNodeWithTag(TAG_RECUPERAR).assertIsNotEnabled()
    }

    @Test
    fun `con correo valido la recuperacion avisa`() {
        var recuperaciones = 0
        pintar(
            formulario = AuthFormState(email = "ash@pueblo-paleta.test"),
            alRecuperar = { recuperaciones++ },
        )

        // `performScrollTo` antes de pulsar: el nodo existe, pero en una
        // ventana de telefono queda por debajo del borde, y Compose no pulsa
        // lo que no se ve.
        compose.onNodeWithTag(TAG_RECUPERAR).performScrollTo().performClick()

        assertEquals(1, recuperaciones)
    }

    @Test
    fun `el aviso de correo enviado no dice si la cuenta existe`() {
        pintar(AuthFormState(email = "ash@pueblo-paleta.test", correoEnviado = true))

        compose.onNodeWithText("Revisa tu correo").assertExists()
        // Lo importante es lo que **no** dice: ni «esa cuenta no existe» ni
        // «te hemos mandado el enlace», que confirmarian que existe.
        val texto = "Si hay una cuenta con ese correo"
        compose.onNodeWithText(texto, substring = true).assertExists()
    }
}
