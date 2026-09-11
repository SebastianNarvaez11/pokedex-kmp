package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.data.network.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.get

/** Lo que se le pide a Supabase **en nombre del usuario**. */
internal interface SupabaseAccountApi {
    suspend fun perfil(): UserDto
}

internal class KtorSupabaseAccountApi(private val client: HttpClient) : SupabaseAccountApi {
    override suspend fun perfil(): UserDto = client.get("user").body()
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

    /** Refresca si o si. Lo llama el cliente cuando el servidor dijo 401. */
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
    return createSupabaseClient(config, engine, extra)
}
