package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.core.AppError
import com.sebastiannarvaez.pokedex.core.BaseAppConfig
import com.sebastiannarvaez.pokedex.core.aAppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Una configuracion de mentira: la URL de PokeAPI la hereda de
// `BaseAppConfig`, y Supabase no hace falta para estos tests.
private class ConfigDePrueba : BaseAppConfig() {
    override val supabaseUrl = ""
    override val supabaseKey = ""
}

class ReintentosTest {

    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun unErrorDelServidorSeReintentaYAcabaComoServidor() = runTest {
        var llamadas = 0
        val motor = MockEngine {                    // este motor contesta siempre 503
            llamadas++
            respond("{}", HttpStatusCode.ServiceUnavailable, json)
        }
        val api = KtorPokeApi(createHttpClient(ConfigDePrueba(), motor))

        val fallo = assertFailsWith<Throwable> { api.page(limit = 20, offset = 0) }

        assertEquals(3, llamadas)                   // el intento inicial mas 2 reintentos
        assertEquals(AppError.Servidor(503), fallo.aAppError())
    }

    @Test
    fun unNoEncontradoNoSeReintenta() = runTest {
        var llamadas = 0
        val motor = MockEngine {
            llamadas++
            respond("{}", HttpStatusCode.NotFound, json)
        }
        val api = KtorPokeApi(createHttpClient(ConfigDePrueba(), motor))

        val fallo = assertFailsWith<Throwable> { api.detail(id = 99999) }

        assertEquals(1, llamadas)                   // un 404 no se vuelve a pedir
        assertEquals(AppError.Servidor(404), fallo.aAppError())
    }

    @Test
    fun unaRedQueSeRecuperaTerminaBien() = runTest {
        var llamadas = 0
        val motor = MockEngine {
            llamadas++
            if (llamadas < 3) throw IOException("socket cerrado")   // falla dos veces...
            respond("""{"count": 0, "results": []}""", HttpStatusCode.OK, json)   // ...y a la tercera va
        }
        val api = KtorPokeApi(createHttpClient(ConfigDePrueba(), motor))

        val pagina = api.page(limit = 20, offset = 0)

        assertEquals(3, llamadas)
        assertEquals(0, pagina.count)
    }
}
