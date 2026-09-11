package com.sebastiannarvaez.pokedex.feature.auth

/**
 * El enlace que llega por correo para recuperar la contrasena.
 *
 * **No es un destino de la app** y por eso no esta en `Destination`. Aquel mapa
 * son pantallas que el usuario puede pedir; esto es un suceso de sesion que
 * ademas trae credenciales. Meterlo alli obligaria a que cada `when` de
 * navegacion tratara un caso que no es una pantalla.
 *
 * Los tokens vienen en el **fragmento** (`#`), no en la consulta (`?`), y eso
 * es a proposito de Supabase: el fragmento **no se envia al servidor** en una
 * peticion HTTP. Si vinieran en la consulta, quedarian en el registro de
 * cualquier servidor por el que pasara el enlace.
 */
data class EnlaceDeRecuperacion(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
) {
    companion object {
        /** La ruta a la que Supabase redirige. Va en `redirect_to`. */
        const val RUTA = "auth/callback"

        fun parse(url: String): EnlaceDeRecuperacion? {
            if (!url.contains(RUTA)) return null
            val fragmento = url.substringAfter('#', missingDelimiterValue = "")
            if (fragmento.isBlank()) return null

            val campos = fragmento.split('&')
                .mapNotNull { trozo ->
                    val clave = trozo.substringBefore('=')
                    val valor = trozo.substringAfter('=', missingDelimiterValue = "")
                    if (clave.isBlank() || valor.isBlank()) null else clave to valor
                }
                .toMap()

            // `type=recovery` distingue este correo del de confirmacion de
            // cuenta, que llega al mismo sitio con la misma forma.
            if (campos["type"] != "recovery") return null

            val acceso = campos["access_token"] ?: return null
            val refresco = campos["refresh_token"] ?: return null
            return EnlaceDeRecuperacion(
                accessToken = acceso,
                refreshToken = refresco,
                expiresIn = campos["expires_in"]?.toLongOrNull() ?: 3600,
            )
        }
    }
}
