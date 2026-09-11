package com.sebastiannarvaez.pokedex.feature.auth

import com.sebastiannarvaez.pokedex.data.network.AuthException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La validacion vive en el nucleo, asi que se prueba una vez y vale para las
 * dos apps. Si estuviera en cada pantalla, habria que escribir estos mismos
 * casos en Kotlin y otra vez en Swift.
 */
class AuthFormStateTest {

    @Test
    fun `un correo sin arroba no vale`() {
        assertFalse(AuthFormState(email = "ash.test", password = "pikachu").emailValido)
    }

    @Test
    fun `un correo sin punto tras la arroba no vale`() {
        assertFalse(AuthFormState(email = "ash@pueblo", password = "pikachu").emailValido)
    }

    @Test
    fun `una contrasena de cinco no llega al minimo de Supabase`() {
        assertFalse(AuthFormState(email = "ash@pueblo.test", password = "12345").passwordValida)
        assertTrue(AuthFormState(email = "ash@pueblo.test", password = "123456").passwordValida)
    }

    @Test
    fun `mientras se envia no se puede volver a enviar`() {
        val valido = AuthFormState(email = "ash@pueblo.test", password = "pikachu")

        assertTrue(valido.sePuedeEnviar)
        assertFalse(valido.copy(enviando = true).sePuedeEnviar)
    }

    /**
     * Un 400 de Supabase es «esa contrasena no es», no «el servidor falla». El
     * mapeo general trataria cualquier 4xx como error de servidor y el usuario
     * leeria que PokeAPI no responde, que no tiene nada que ver.
     */
    @Test
    fun `el error de credenciales conserva el mensaje del servidor`() {
        val error = AuthException(400, "Invalid login credentials").aUiErrorDeAuth()

        assertEquals("Invalid login credentials", error.detalle)
        assertFalse(error.sePuedeReintentar)
    }
}
