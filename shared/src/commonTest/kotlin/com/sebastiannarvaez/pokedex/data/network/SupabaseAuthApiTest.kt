package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.dobles.ConfigDePrueba
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Respuesta real de `/auth/v1/token?grant_type=password`, recortada. */
private val SESION_JSON = """
{
  "access_token": "eyJhbGciOiJIUzI1NiIs.token",
  "token_type": "bearer",
  "expires_in": 3600,
  "expires_at": 1789000000,
  "refresh_token": "v1kqNrTz",
  "user": {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "email": "ash@pueblo-paleta.test",
    "role": "authenticated"
  }
}
""".trimIndent()

private fun json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK) = MockEngine { _ ->
    respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
}

class SupabaseAuthApiTest {

    @Test
    fun `entrar devuelve la sesion`() = runTest {
        val api = KtorSupabaseAuthApi(createSupabaseClient(ConfigDePrueba(), json(SESION_JSON)))

        val sesion = api.entrar("ash@pueblo-paleta.test", "pikachu")

        assertEquals("eyJhbGciOiJIUzI1NiIs.token", sesion.accessToken)
        assertEquals("v1kqNrTz", sesion.refreshToken)
        assertEquals(3600, sesion.expiresIn)
        assertEquals("ash@pueblo-paleta.test", sesion.user?.email)
    }

    @Test
    fun `la peticion lleva la apikey y el grant_type`() = runTest {
        val motor = json(SESION_JSON)
        val api = KtorSupabaseAuthApi(createSupabaseClient(ConfigDePrueba(), motor))

        api.entrar("ash@pueblo-paleta.test", "pikachu")

        val peticion = motor.requestHistory.single()
        assertEquals("clave-publica", peticion.headers["apikey"])
        assertEquals("password", peticion.url.parameters["grant_type"])
        assertEquals("/auth/v1/token", peticion.url.encodedPath)
    }

    @Test
    fun `refrescar usa el otro grant_type`() = runTest {
        val motor = json(SESION_JSON)
        val api = KtorSupabaseAuthApi(createSupabaseClient(ConfigDePrueba(), motor))

        api.refrescar("v1kqNrTz")

        assertEquals("refresh_token", motor.requestHistory.single().url.parameters["grant_type"])
    }

    /**
     * Supabase no usa siempre el mismo campo para el mensaje: el login malo
     * devuelve `error_description`, el registro repetido devuelve `msg`. Si el
     * DTO exigiera uno concreto, el error del usuario se convertiria en un
     * fallo de deserializacion.
     */
    @Test
    fun `un error con error_description llega al mensaje`() = runTest {
        val cuerpo = """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""
        val api = KtorSupabaseAuthApi(
            createSupabaseClient(ConfigDePrueba(), json(cuerpo, HttpStatusCode.BadRequest)),
        )

        val e = assertFailsWith<AuthException> { api.entrar("ash@pueblo-paleta.test", "malo") }

        assertEquals(400, e.codigo)
        assertEquals("Invalid login credentials", e.message)
    }

    @Test
    fun `un error con msg tambien`() = runTest {
        val cuerpo = """{"code":422,"msg":"User already registered"}"""
        val api = KtorSupabaseAuthApi(
            createSupabaseClient(ConfigDePrueba(), json(cuerpo, HttpStatusCode.UnprocessableEntity)),
        )

        val e = assertFailsWith<AuthException> { api.registrar("ash@pueblo-paleta.test", "pikachu") }

        assertEquals("User already registered", e.message)
    }

    /** Un cuerpo que no se entiende no debe tapar el codigo HTTP. */
    @Test
    fun `un error sin cuerpo conocido deja un mensaje generico`() = runTest {
        val api = KtorSupabaseAuthApi(
            createSupabaseClient(ConfigDePrueba(), json("<html>502</html>", HttpStatusCode.BadGateway)),
        )

        val e = assertFailsWith<AuthException> { api.entrar("ash@pueblo-paleta.test", "pikachu") }

        assertEquals(502, e.codigo)
        assertTrue(e.message!!.contains("502"))
    }

    /** Si el servidor no deja cerrar sesion, la app no se entera: ya se fue. */
    @Test
    fun `salir no lanza aunque el servidor falle`() = runTest {
        val api = KtorSupabaseAuthApi(
            createSupabaseClient(ConfigDePrueba(), json("{}", HttpStatusCode.InternalServerError)),
        )

        api.salir("token")
    }
}
