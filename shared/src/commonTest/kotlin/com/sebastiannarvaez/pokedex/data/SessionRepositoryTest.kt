package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.data.network.SupabaseAuthApi
import com.sebastiannarvaez.pokedex.data.network.dto.SessionDto
import com.sebastiannarvaez.pokedex.data.network.dto.UserDto
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import com.sebastiannarvaez.pokedex.domain.Session
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El doble del servidor.
 *
 * `expiresInTrasRefresco` no es un capricho: si el refresco devolviera la misma
 * duracion corta, con el reloj congelado del test el token nuevo tambien
 * naceria caducado y el repositorio refrescaria en bucle. Un servidor de
 * verdad nunca devuelve un token ya vencido.
 */
private class FakeAuthApi(
    private val expiresIn: Long = 3600,
    private val expiresInTrasRefresco: Long = 3600,
    private val fallaElRefresco: Boolean = false,
) : SupabaseAuthApi {

    var refrescos = 0
        private set
    var cierresEnElServidor = 0
        private set

    override suspend fun registrar(email: String, password: String) = sesion("registro", expiresIn)
    override suspend fun entrar(email: String, password: String) = sesion("entrada", expiresIn)

    override suspend fun refrescar(refreshToken: String): SessionDto {
        refrescos++
        if (fallaElRefresco) error("refresh token ya usado")
        return sesion("refresco-$refrescos", expiresInTrasRefresco)
    }

    override suspend fun salir(accessToken: String) {
        cierresEnElServidor++
    }

    private fun sesion(marca: String, expiresIn: Long) = SessionDto(
        accessToken = "access-$marca",
        refreshToken = "refresh-$marca",
        expiresIn = expiresIn,
        tokenType = "bearer",
        user = UserDto(id = "u-1", email = "ash@pueblo-paleta.test"),
    )
}

class SessionRepositoryTest {

    /**
     * Es extension de `TestScope` por una razon concreta.
     *
     * Crear el dispatcher con `StandardTestDispatcher()` a secas le da un
     * planificador propio, distinto del que usa `runTest`, y el test muere con
     * «Detected use of different schedulers». El planificador del test se
     * hereda pasando `testScheduler`.
     */
    private fun TestScope.repo(
        api: SupabaseAuthApi = FakeAuthApi(),
        store: TokenStore = InMemoryTokenStore(),
        ahora: () -> Long = { 1_000 },
    ) = SessionRepository(api, store, TestDispatchers(StandardTestDispatcher(testScheduler)), ahora)

    @Test
    fun `al arrancar sin nada guardado no hay sesion`() = runTest {
        val r = repo()

        assertTrue(r.state.value.comprobando)
        r.restaurar()

        assertEquals(false, r.state.value.comprobando)
        assertNull(r.state.value.session)
    }

    @Test
    fun `restaurar recupera lo que habia guardado`() = runTest {
        val store = InMemoryTokenStore()
        store.guardar(Session("a", "r", 99_999, "u-1", "ash@pueblo-paleta.test"))

        val r = repo(store = store)
        r.restaurar()

        assertEquals("a", r.state.value.session?.accessToken)
        assertTrue(r.state.value.haySesion)
    }

    /**
     * Lo importante no es el token, es la fecha: el servidor manda `expires_in`
     * y aqui se convierte en un instante. Guardar los 3600 tal cual haria que
     * la sesion pareciera valida para siempre.
     */
    @Test
    fun `entrar convierte expires_in en instante de caducidad`() = runTest {
        val r = repo(ahora = { 1_000 })

        r.entrar("ash@pueblo-paleta.test", "pikachu")

        assertEquals(4_600, r.state.value.session?.expiresAtEpochSeconds)
    }

    @Test
    fun `entrar deja la sesion guardada para el proximo arranque`() = runTest {
        val store = InMemoryTokenStore()
        repo(store = store).entrar("ash@pueblo-paleta.test", "pikachu")

        assertEquals("access-entrada", store.leer()?.accessToken)
    }

    @Test
    fun `salir borra el token y avisa al servidor`() = runTest {
        val api = FakeAuthApi()
        val store = InMemoryTokenStore()
        val r = repo(api, store)
        r.entrar("ash@pueblo-paleta.test", "pikachu")

        r.salir()

        assertNull(r.state.value.session)
        assertNull(store.leer())
        assertEquals(1, api.cierresEnElServidor)
    }

    @Test
    fun `un token vigente no se refresca`() = runTest {
        val api = FakeAuthApi()
        val r = repo(api, ahora = { 1_000 })
        r.entrar("ash@pueblo-paleta.test", "pikachu")

        assertEquals("access-entrada", r.tokenValido())
        assertEquals(0, api.refrescos)
    }

    @Test
    fun `un token a punto de caducar se refresca`() = runTest {
        val api = FakeAuthApi(expiresIn = 30)
        val r = repo(api, ahora = { 1_000 })
        r.entrar("ash@pueblo-paleta.test", "pikachu")

        assertEquals("access-refresco-1", r.tokenValido())
        assertEquals(1, api.refrescos)
    }

    /**
     * El caso que justifica el `Mutex`.
     *
     * El token de refresco de Supabase es de un solo uso. Si dos peticiones
     * caducadas lo gastan a la vez, la segunda recibe 401 y tira la sesion
     * entera. Aqui se lanzan cinco y solo puede haber un refresco.
     */
    @Test
    fun `cinco peticiones a la vez refrescan una sola vez`() = runTest {
        val api = FakeAuthApi(expiresIn = 30)
        val r = repo(api, ahora = { 1_000 })
        r.entrar("ash@pueblo-paleta.test", "pikachu")

        // De verdad a la vez: cinco corrutinas lanzadas antes de esperar a
        // ninguna. Llamarlas en un bucle seria secuencial, y entonces el test
        // pasaria incluso sin el candado.
        val tokens = List(5) { async { r.tokenValido() } }.awaitAll()

        assertEquals(1, api.refrescos)
        assertTrue(tokens.all { it == "access-refresco-1" })
    }

    @Test
    fun `si el refresco falla la sesion se cierra`() = runTest {
        val store = InMemoryTokenStore()
        val r = repo(FakeAuthApi(expiresIn = 30, fallaElRefresco = true), store, ahora = { 1_000 })
        r.entrar("ash@pueblo-paleta.test", "pikachu")

        assertFailsWith<IllegalStateException> { r.tokenValido() }

        assertNull(r.state.value.session)
        assertNull(store.leer())
    }

    @Test
    fun `sin sesion no hay token que dar`() = runTest {
        assertNull(repo().tokenValido())
    }
}
