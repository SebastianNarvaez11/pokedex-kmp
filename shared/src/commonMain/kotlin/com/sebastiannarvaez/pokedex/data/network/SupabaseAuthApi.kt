package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.core.Log
import com.sebastiannarvaez.pokedex.data.network.dto.AuthErrorDto
import com.sebastiannarvaez.pokedex.data.network.dto.SessionDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Lo que la app le pide a Supabase. */
internal interface SupabaseAuthApi {
    suspend fun registrar(email: String, password: String): SessionDto
    suspend fun entrar(email: String, password: String): SessionDto
    suspend fun refrescar(refreshToken: String): SessionDto
    suspend fun salir(accessToken: String)
}

@Serializable
private data class CredencialesDto(val email: String, val password: String)

@Serializable
private data class RefrescoDto(val refresh_token: String)

/**
 * Error de autenticacion con el mensaje que devolvio el servidor.
 *
 * Se lanza a proposito en vez de devolver un resultado: el mapeo a `UiError`
 * ya existe y trata cualquier `Throwable`, asi que anadir un tipo de resultado
 * aqui duplicaria el camino de los errores.
 */
internal class AuthException(val codigo: Int, mensaje: String?) :
    Exception(mensaje ?: "error de autenticación ($codigo)")

internal class KtorSupabaseAuthApi(private val client: HttpClient) : SupabaseAuthApi {

    override suspend fun registrar(email: String, password: String): SessionDto =
        client.post("signup") {
            contentType(ContentType.Application.Json)
            setBody(CredencialesDto(email, password))
        }.sesionOError()

    override suspend fun entrar(email: String, password: String): SessionDto =
        client.post("token") {
            parameter("grant_type", "password")
            contentType(ContentType.Application.Json)
            setBody(CredencialesDto(email, password))
        }.sesionOError()

    override suspend fun refrescar(refreshToken: String): SessionDto =
        client.post("token") {
            parameter("grant_type", "refresh_token")
            contentType(ContentType.Application.Json)
            setBody(RefrescoDto(refreshToken))
        }.sesionOError()

    override suspend fun salir(accessToken: String) {
        // Si falla, da igual: el token local se borra de todos modos. Un cierre
        // de sesion que falla por falta de red no debe dejar al usuario dentro.
        runCatching {
            client.post("logout") { header("Authorization", "Bearer $accessToken") }
        }.onFailure { Log.w("no se pudo cerrar sesion en el servidor", it) }
    }

    private suspend fun HttpResponse.sesionOError(): SessionDto {
        if (status.isSuccess()) return body()
        val error = runCatching { body<AuthErrorDto>() }.getOrNull()
        throw AuthException(status.value, error?.mensaje)
    }
}

/**
 * Un cliente **aparte** para Supabase.
 *
 * No se reutiliza el de PokeAPI a proposito: comparten configuracion pero no
 * destino, y un solo cliente con dos URL base obligaria a repetir la direccion
 * en cada peticion. Peor aun, la cabecera `apikey` y el token de sesion
 * acabarian viajando tambien a PokeAPI, que no tiene por que verlos.
 */
internal fun createSupabaseClient(
    config: AppConfig,
    engine: HttpClientEngine? = null,
    extra: io.ktor.client.HttpClientConfig<*>.() -> Unit = {},
): HttpClient {
    val configuracion: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(config.supabaseUrl.trimEnd('/') + "/auth/v1/")
            // `apikey` en todas las peticiones: sin ella, Supabase responde 401
            // aunque el token de sesion sea correcto.
            header("apikey", config.supabaseKey)
        }
        extra()
    }

    return if (engine == null) HttpClient(configuracion) else HttpClient(engine, configuracion)
}
