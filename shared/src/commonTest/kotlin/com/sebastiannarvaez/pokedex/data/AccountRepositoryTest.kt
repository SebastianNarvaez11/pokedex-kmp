package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.data.network.SupabaseAccountApi
import com.sebastiannarvaez.pokedex.data.network.dto.UserDto
import com.sebastiannarvaez.pokedex.dobles.FakeAuthApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import com.sebastiannarvaez.pokedex.feature.auth.EnlaceDeRecuperacion
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAccountApi(private val fallaElBorrado: Boolean = false) : SupabaseAccountApi {
    var passwordsCambiadas = 0
        private set
    var borrados = 0
        private set

    override suspend fun perfil() = UserDto(id = "u-1", email = "ash@pueblo-paleta.test")

    override suspend fun cambiarPassword(nueva: String) {
        passwordsCambiadas++
    }

    override suspend fun borrarCuenta() {
        borrados++
        if (fallaElBorrado) error("la función no existe")
    }
}

class AccountRepositoryTest {

    private fun TestScope.sesion() = SessionRepository(
        FakeAuthApi(),
        InMemoryTokenStore(),
        TestDispatchers(StandardTestDispatcher(testScheduler)),
        ahoraEnSegundos = { 1_000 },
    )

    private fun TestScope.cuenta(api: SupabaseAccountApi, session: SessionRepository) =
        AccountRepository(api, session, TestDispatchers(StandardTestDispatcher(testScheduler)))

    @Test
    fun `borrar la cuenta cierra la sesion`() = runTest {
        val session = sesion()
        session.entrar("ash@pueblo-paleta.test", "pikachu")
        val api = FakeAccountApi()

        cuenta(api, session).borrarCuenta()

        assertEquals(1, api.borrados)
        assertNull(session.state.value.session)
    }

    /**
     * El orden importa. Si se cerrara la sesion antes de borrar y el borrado
     * fallara, el usuario quedaria fuera de una cuenta que sigue existiendo, y
     * sin token para reintentar.
     */
    @Test
    fun `si el borrado falla la sesion sigue viva`() = runTest {
        val session = sesion()
        session.entrar("ash@pueblo-paleta.test", "pikachu")

        assertFailsWith<IllegalStateException> {
            cuenta(FakeAccountApi(fallaElBorrado = true), session).borrarCuenta()
        }

        assertTrue(session.state.value.haySesion)
    }

    @Test
    fun `el enlace del correo deja al usuario dentro`() = runTest {
        val session = sesion()

        session.adoptar(EnlaceDeRecuperacion("acceso-temporal", "refresco", 3600))

        assertTrue(session.state.value.haySesion)
        assertEquals("acceso-temporal", session.state.value.session?.accessToken)
        assertEquals(4_600, session.state.value.session?.expiresAtEpochSeconds)
    }

    @Test
    fun `la recuperacion apunta al esquema propio de la app`() = runTest {
        val api = FakeAuthApi()
        val session = SessionRepository(
            api,
            InMemoryTokenStore(),
            TestDispatchers(StandardTestDispatcher(testScheduler)),
        )

        session.recuperar("ash@pueblo-paleta.test", "pokedex://auth/callback")

        assertEquals(1, api.recuperaciones)
        assertNotNull(api.ultimaRedireccion)
        assertTrue(api.ultimaRedireccion!!.startsWith("pokedex://"))
    }
}
