package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.core.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
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
    configurar: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                // PokeAPI devuelve decenas de campos que no usamos. Sin esto,
                // cada campo nuevo que anadan rompe la app en produccion.
                ignoreUnknownKeys = true
            },
        )
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

    configurar()
}
