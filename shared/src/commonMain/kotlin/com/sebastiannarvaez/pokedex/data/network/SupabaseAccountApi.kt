package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.data.network.dto.AuthErrorDto
import com.sebastiannarvaez.pokedex.data.network.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.plugin
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/** Lo que se le pide a Supabase **en nombre del usuario**. */
internal interface SupabaseAccountApi {
    suspend fun perfil(): UserDto
    suspend fun cambiarPassword(nueva: String)
    suspend fun borrarCuenta()
}

@Serializable
private data class PasswordDto(val password: String)

internal class KtorSupabaseAccountApi(
    private val client: HttpClient,
    private val config: AppConfig,
) : SupabaseAccountApi {

    override suspend fun perfil(): UserDto = client.get("user").body()

    override suspend fun cambiarPassword(nueva: String) {
        client.put("user") {
            contentType(ContentType.Application.Json)
            setBody(PasswordDto(nueva))
        }.exigirExito()
    }

    /**
     * Borrar la propia cuenta **no tiene endpoint**.
     *
     * Supabase no expone «elimina mi usuario» en la API publica: hacerlo seria
     * dar a cualquier token la capacidad de borrar filas de una tabla del
     * sistema. La via documentada es una funcion SQL `security definer` que
     * comprueba quien llama y borra su propio usuario, publicada como
     * procedimiento remoto.
     *
     * La URL es absoluta porque el cliente apunta a `/auth/v1/` y esto vive en
     * `/rest/v1/`, que es otra API del mismo proyecto.
     */
    override suspend fun borrarCuenta() {
        client.post(config.supabaseUrl.trimEnd('/') + "/rest/v1/rpc/delete_user")
            .exigirExito()
    }

    private suspend fun HttpResponse.exigirExito() {
        if (status.isSuccess()) return
        val error = runCatching { body<AuthErrorDto>() }.getOrNull()
        throw AuthException(status.value, error?.mensaje)
    }
}

/**
 * De donde saca el cliente los tokens.
 *
 * Una interfaz de una sola funcion, y no el repositorio entero, porque rompe el
 * ciclo: el cliente necesita tokens, los tokens los da el repositorio, y el
 * repositorio necesita un cliente. Con esto, el cliente depende de una funcion
 * que alguien le pasa, no de quien la implementa.
 */
internal interface ProveedorDeTokens {
    /** El par vigente, refrescando solo si toca por reloj. */
    suspend fun vigentes(): Pair<String, String>?

    /** Refresca si o si. Lo llama el cliente cuando el servidor dijo 401 o 403. */
    suspend fun refrescar(): Pair<String, String>?
}

/**
 * El **tercer** cliente de la app, y el unico que lleva sesion.
 *
 * Son tres a proposito:
 *
 * - PokeAPI, que no sabe nada de cuentas.
 * - Supabase sin sesion, para entrar, registrarse y refrescar.
 * - Este, para lo que se hace en nombre del usuario.
 *
 * Separar los dos de Supabase no es cosmetico. Si el plugin de sesion
 * estuviera tambien en el que refresca, un 401 del propio refresco dispararia
 * otro refresco, que volveria a dar 401, que dispararia otro. La recursion no
 * la corta ningun `sendWithoutRequest`, porque el fallo ocurre en la peticion
 * que el propio plugin lanza.
 */
internal fun createSupabaseAccountClient(
    config: AppConfig,
    proveedor: ProveedorDeTokens,
    engine: HttpClientEngine? = null,
): HttpClient {
    val extra: HttpClientConfig<*>.() -> Unit = {
        install(Auth) {
            bearer {
                // Se llama una vez y Ktor cachea el resultado. El refresco
                // proactivo lo hace el repositorio, asi que aqui basta pedirle
                // el par vigente.
                loadTokens {
                    proveedor.vigentes()?.let { (acceso, refresco) -> BearerTokens(acceso, refresco) }
                }
                // Solo cuando el servidor ya ha dicho 401. Y aqui hay que
                // **forzar** el refresco: si se volviera a preguntar «segun el
                // reloj, sigue valiendo?», la respuesta seria que si, se
                // devolveria el mismo token rechazado y la peticion fallaria
                // otra vez.
                refreshTokens {
                    proveedor.refrescar()?.let { (acceso, refresco) -> BearerTokens(acceso, refresco) }
                }
                // Sin esto, Ktor manda la peticion sin cabecera, espera el 401 y
                // la repite: dos viajes por cada llamada. El filtro por host
                // ademas garantiza que el token no salga hacia PokeAPI ni hacia
                // ningun otro sitio.
                sendWithoutRequest { peticion ->
                    peticion.url.host == config.supabaseUrl.substringAfter("://").trimEnd('/')
                }
            }
        }
    }
    val cliente = createSupabaseClient(config, engine, extra)

    /*
     * El 403 que el plugin de Ktor no ve.
     *
     * Ktor refresca cuando el servidor contesta **401**, y solo entonces: «The
     * resource server returns a 401 Unauthorized response. The client
     * automatically invokes the refreshTokens {} callback». Supabase no se
     * comporta igual en sus dos mitades, medido contra un proyecto real el 11
     * de septiembre de 2026 con la caducidad puesta en 60 segundos:
     *
     *   GET  /auth/v1/user            -> 403 {"error_code":"bad_jwt"}
     *   POST /rest/v1/rpc/delete_user -> 401 {"code":"PGRST303"}
     *
     * Asi que el refresco automatico funcionaba en las llamadas REST y no en
     * las de autenticacion, que son las mas frecuentes. Esto lo iguala.
     *
     * Se filtra por `/auth/v1/` a proposito: en `/rest/v1/` un 403 significa
     * que una regla de seguridad de filas dijo que no, y refrescar ahi seria
     * gastar un token de refresco por cada negativa legitima.
     *
     * No se lee el cuerpo para confirmar que es `bad_jwt`: consumirlo aqui
     * dejaria sin cuerpo a quien recibe la respuesta cuando el 403 sea de otra
     * cosa. Con el filtro por ruta, el unico 403 que llega es ese.
     */
    cliente.plugin(HttpSend).intercept { peticion ->
        val llamada = execute(peticion)
        if (llamada.response.status != HttpStatusCode.Forbidden) return@intercept llamada
        if (!peticion.url.buildString().contains("/auth/v1/")) return@intercept llamada

        val nuevos = proveedor.refrescar() ?: return@intercept llamada

        // El proveedor de Ktor cachea lo que devolvio `loadTokens`. Sin
        // limpiarlo, la siguiente peticion volveria a firmar con el token que
        // el servidor acaba de rechazar.
        cliente.authProvider<BearerAuthProvider>()?.clearToken()

        peticion.headers[HttpHeaders.Authorization] = "Bearer ${nuevos.first}"
        execute(peticion)
    }

    return cliente
}
