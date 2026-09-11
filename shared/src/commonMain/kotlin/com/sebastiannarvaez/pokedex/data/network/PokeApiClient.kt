package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.core.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * El cliente HTTP de la app, configurado en un solo sitio.
 *
 * El motor no se elige aqui: Ktor separa el cliente del motor a proposito, y
 * cada plataforma aporta el suyo —OkHttp en Android, Darwin en iOS— sin que
 * este codigo se entere.
 */
internal fun createHttpClient(
    config: AppConfig,
    /**
     * El motor. En produccion se deja a null y cada plataforma pone el suyo;
     * en los tests se pasa un `MockEngine`, y asi lo que se prueba es **esta
     * misma configuracion**, no una montada aparte.
     */
    engine: HttpClientEngine? = null,
): HttpClient {
    val configuracion: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            json(
                Json {
                    // PokeAPI devuelve decenas de campos que no usamos. Sin esto,
                    // cada campo nuevo que anadan rompe la app en produccion.
                    ignoreUnknownKeys = true
                },
            )
        }

        // Reintenta lo que merece la pena reintentar: fallos de red y errores
        // del servidor. Un 404 no se reintenta, porque va a seguir siendo 404.
        //
        // La espera crece entre intentos. Sin eso, tres reintentos inmediatos
        // contra un servidor caido son tres peticiones mas que no ayudan a que
        // se levante.
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryOnExceptionIf(maxRetries = 2) { _, causa -> causa !is CancellationException }
            exponentialDelay(base = 2.0, maxDelayMs = 4_000)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) = Log.d(message)
            }
            level = LogLevel.INFO
        }

        defaultRequest {
            url(config.pokeApiBaseUrl)
            this.url.protocol = URLProtocol.HTTPS
        }
    }

    return if (engine == null) HttpClient(configuracion) else HttpClient(engine, configuracion)
}
