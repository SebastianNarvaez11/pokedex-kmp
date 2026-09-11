package com.sebastiannarvaez.pokedex.feature.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Un enlace real de Supabase, con los tokens acortados. */
private const val REAL =
    "pokedex://auth/callback#access_token=eyJhbGci.abc&expires_at=1789000000" +
        "&expires_in=3600&refresh_token=v1kqNrTz&token_type=bearer&type=recovery"

class EnlaceDeRecuperacionTest {

    @Test
    fun `saca los dos tokens del fragmento`() {
        val enlace = EnlaceDeRecuperacion.parse(REAL)

        assertEquals("eyJhbGci.abc", enlace?.accessToken)
        assertEquals("v1kqNrTz", enlace?.refreshToken)
        assertEquals(3600, enlace?.expiresIn)
    }

    /**
     * El correo de confirmacion de cuenta llega al mismo sitio con la misma
     * forma. Sin mirar `type`, abrir ese correo lanzaria la pantalla de
     * contrasena nueva a quien solo estaba confirmando su registro.
     */
    @Test
    fun `un enlace de confirmacion no es de recuperacion`() {
        val confirmacion = REAL.replace("type=recovery", "type=signup")

        assertNull(EnlaceDeRecuperacion.parse(confirmacion))
    }

    @Test
    fun `un enlace normal de la app no es de recuperacion`() {
        assertNull(EnlaceDeRecuperacion.parse("pokedex://pokemon/25"))
    }

    @Test
    fun `sin fragmento no hay nada que adoptar`() {
        assertNull(EnlaceDeRecuperacion.parse("pokedex://auth/callback"))
    }

    /** Falta el token de refresco: media sesion no sirve de nada. */
    @Test
    fun `un fragmento incompleto se descarta entero`() {
        val incompleto = "pokedex://auth/callback#access_token=abc&type=recovery"

        assertNull(EnlaceDeRecuperacion.parse(incompleto))
    }

    @Test
    fun `si falta expires_in se asume una hora`() {
        val sinDuracion = REAL.replace("&expires_in=3600", "")

        assertEquals(3600, EnlaceDeRecuperacion.parse(sinDuracion)?.expiresIn)
    }
}
