package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.core.AppConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeConfig : AppConfig {
    override val pokeApiBaseUrl = "https://pokeapi.co/api/v2/"
}

/**
 * Recorte de una respuesta real de PokeAPI.
 *
 * La de verdad ocupa **272 KB** y tiene decenas de campos que la app no usa.
 * Este recorte conserva a proposito dos de ellos —`base_experience` y
 * `sprites`— para que el test falle si alguien quita `ignoreUnknownKeys`.
 */
private val DETALLE_JSON = """
{
  "id": 1,
  "name": "bulbasaur",
  "base_experience": 64,
  "height": 7,
  "weight": 69,
  "sprites": { "front_default": "https://example.invalid/1.png" },
  "types": [
    { "slot": 2, "type": { "name": "poison", "url": "https://pokeapi.co/api/v2/type/4/" } },
    { "slot": 1, "type": { "name": "grass", "url": "https://pokeapi.co/api/v2/type/12/" } }
  ]
}
""".trimIndent()

private val PAGINA_JSON = """
{
  "count": 1351,
  "next": "https://pokeapi.co/api/v2/pokemon?offset=2&limit=2",
  "previous": null,
  "results": [
    { "name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/" },
    { "name": "ivysaur", "url": "https://pokeapi.co/api/v2/pokemon/2/" }
  ]
}
""".trimIndent()

class KtorPokeApiTest {

    private fun api(respuesta: String, capturar: (String) -> Unit = {}): PokeApi {
        val engine = MockEngine { peticion ->
            capturar(peticion.url.toString())
            respond(
                content = respuesta,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        // MockEngine sustituye al motor de la plataforma sin tocar nada mas:
        // la configuracion que se prueba es exactamente la de produccion.
        return KtorPokeApi(createHttpClient(FakeConfig(), engine = engine))
    }

    @Test
    fun elDetalleIgnoraLosCamposQueNoUsamos() = runTest {
        val detalle = api(DETALLE_JSON).detail(1)

        assertEquals(1, detalle.id)
        assertEquals("bulbasaur", detalle.name)
        assertEquals(7, detalle.height)
        assertEquals(listOf("poison", "grass"), detalle.types.map { it.type.name })
    }

    @Test
    fun elListadoConstruyeLaUrlConLimiteYDesplazamiento() = runTest {
        var pedida = ""
        val pagina = api(PAGINA_JSON, capturar = { pedida = it }).page(limit = 2, offset = 4)

        assertTrue(pedida.contains("limit=2"), "url inesperada: $pedida")
        assertTrue(pedida.contains("offset=4"), "url inesperada: $pedida")
        assertTrue(pedida.startsWith("https://pokeapi.co/api/v2/pokemon"), "url inesperada: $pedida")
        assertEquals(1351, pagina.count)
    }

    @Test
    fun elIdentificadorSeSacaDeLaUrlDelListado() = runTest {
        val pagina = api(PAGINA_JSON).page(limit = 2, offset = 0)

        assertEquals(listOf(1, 2), pagina.results.map { it.id })
    }
}
