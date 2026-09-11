package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.data.InMemoryTokenStore
import com.sebastiannarvaez.pokedex.data.SessionRepository
import com.sebastiannarvaez.pokedex.dobles.ConfigDePrueba
import com.sebastiannarvaez.pokedex.dobles.FakeAuthApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val PERFIL = """{"id":"u-1","email":"ash@pueblo-paleta.test"}"""
private val JSON = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

class SupabaseAccountApiTest {

    private fun TestScope.sesion(api: FakeAuthApi) = SessionRepository(
        api,
        InMemoryTokenStore(),
        TestDispatchers(StandardTestDispatcher(testScheduler)),
        ahoraEnSegundos = { 1_000 },
    )

    private fun proveedor(repo: SessionRepository) = object : ProveedorDeTokens {
        override suspend fun vigentes() = repo.tokensVigentes()
        override suspend fun refrescar() = repo.refrescarAhora()
    }

    @Test
    fun `la peticion sale ya con la cabecera y no espera al 401`() = runTest {
        val motor = MockEngine { respond(PERFIL, HttpStatusCode.OK, JSON) }
        val repo = sesion(FakeAuthApi())
        repo.entrar("ash@pueblo-paleta.test", "pikachu")

        KtorSupabaseAccountApi(
            createSupabaseAccountClient(ConfigDePrueba(), proveedor(repo), motor),
        ).perfil()

        // Una sola peticion: si `sendWithoutRequest` no filtrara por host,
        // Ktor mandaria una sin cabecera, recibiria 401 y repetiria.
        assertEquals(1, motor.requestHistory.size)
        assertEquals("Bearer access-entrada", motor.requestHistory.single().headers[HttpHeaders.Authorization])
    }

    /**
     * El caso que justifica el plugin.
     *
     * El reloj dice que el token vale, pero el servidor lo rechaza. Puede pasar
     * por mil motivos, y ninguno se ve desde el movil.
     */
    @Test
    fun `un 401 refresca y repite la peticion con el token nuevo`() = runTest {
        var primera = true
        val motor = MockEngine {
            if (primera) {
                primera = false
                respond("""{"message":"invalid JWT"}""", HttpStatusCode.Unauthorized, JSON)
            } else {
                respond(PERFIL, HttpStatusCode.OK, JSON)
            }
        }
        val api = FakeAuthApi()
        val repo = sesion(api)
        repo.entrar("ash@pueblo-paleta.test", "pikachu")

        val perfil = KtorSupabaseAccountApi(
            createSupabaseAccountClient(ConfigDePrueba(), proveedor(repo), motor),
        ).perfil()

        assertEquals("ash@pueblo-paleta.test", perfil.email)
        assertEquals(1, api.refrescos)
        assertEquals(2, motor.requestHistory.size)
        assertEquals("Bearer access-entrada", motor.requestHistory[0].headers[HttpHeaders.Authorization])
        assertEquals("Bearer access-refresco-1", motor.requestHistory[1].headers[HttpHeaders.Authorization])
        // Y la sesion guardada tambien queda actualizada, no solo la del cliente.
        assertEquals("access-refresco-1", repo.state.value.session?.accessToken)
    }

    /**
     * Dos peticiones caducadas a la vez: el token de refresco es de un solo
     * uso, asi que solo puede haber un refresco. Aqui se comprueba de punta a
     * punta, con el plugin de por medio y no solo el repositorio.
     */
    @Test
    fun `tres 401 a la vez refrescan una sola vez`() = runTest {
        val motor = MockEngine { peticion ->
            if (peticion.headers[HttpHeaders.Authorization] == "Bearer access-entrada") {
                respond("""{"message":"invalid JWT"}""", HttpStatusCode.Unauthorized, JSON)
            } else {
                respond(PERFIL, HttpStatusCode.OK, JSON)
            }
        }
        val api = FakeAuthApi()
        val repo = sesion(api)
        repo.entrar("ash@pueblo-paleta.test", "pikachu")
        val cuenta = KtorSupabaseAccountApi(
            createSupabaseAccountClient(ConfigDePrueba(), proveedor(repo), motor),
        )

        val perfiles = List(3) { async { cuenta.perfil() } }.awaitAll()

        assertTrue(perfiles.all { it.id == "u-1" })
        assertEquals(1, api.refrescos)
    }

    /**
     * La razon de que este cliente sea otro y no el de PokeAPI.
     *
     * `sendWithoutRequest` filtra por host: una llamada a cualquier otro sitio
     * sale sin cabecera. El token de un usuario no tiene por que pasar por los
     * servidores de un tercero.
     */
    @Test
    fun `el token no sale hacia otro dominio`() = runTest {
        val motor = MockEngine { respond("[]", HttpStatusCode.OK, JSON) }
        val repo = sesion(FakeAuthApi())
        repo.entrar("ash@pueblo-paleta.test", "pikachu")
        val cliente = createSupabaseAccountClient(ConfigDePrueba(), proveedor(repo), motor)

        cliente.get("https://pokeapi.co/api/v2/pokemon/25").bodyAsText()

        assertNull(motor.requestHistory.single().headers[HttpHeaders.Authorization])
    }
}
