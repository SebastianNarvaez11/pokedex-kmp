package com.sebastiannarvaez.pokedex.data

import android.content.Context

/**
 * El almacen privado de la app en Android.
 *
 * **Por que no `EncryptedSharedPreferences`:** AndroidX la marco obsoleta junto
 * con toda `androidx.security.crypto`, y la recomendacion oficial es usar las
 * APIs de la plataforma y el Keystore directamente. Anadir hoy una dependencia
 * obsoleta para «cifrar» seria empezar con deuda.
 *
 * **Que protege esto entonces:** el fichero vive en el sandbox de la app, con
 * permisos de su propio usuario de Linux. Ninguna otra app lo lee. Lo que no
 * protege es un movil con root o un volcado fisico; para eso hace falta el
 * Keystore, y en la practica se combina con `setUserAuthenticationRequired`,
 * que exige huella y cambia la experiencia de la app entera.
 *
 * Para un token de sesion que caduca en una hora y se puede revocar desde el
 * servidor, el sandbox mas excluirlo de la copia de seguridad es la eleccion
 * razonable. Lo que **no** vale es dejarlo en un fichero del almacenamiento
 * compartido ni en los logs.
 */
internal class AndroidSecureStorage(context: Context) : SecureStorage {

    private val prefs = context.applicationContext
        .getSharedPreferences(FICHERO, Context.MODE_PRIVATE)

    override fun leer(clave: String): String? = prefs.getString(clave, null)

    // `commit()` y no `apply()`: `apply()` escribe en otro hilo y devuelve al
    // momento. Si la app muere justo despues de entrar, la sesion se pierde y
    // el sintoma es «a veces no me guarda la sesion», que es de los peores.
    // Aqui ya estamos en el dispatcher de entrada y salida.
    override fun guardar(clave: String, valor: String) {
        prefs.edit().putString(clave, valor).commit()
    }

    override fun borrar(clave: String) {
        prefs.edit().remove(clave).commit()
    }

    private companion object {
        /**
         * Fichero aparte de los ajustes, y con nombre propio: asi se puede
         * excluir de la copia de seguridad sin excluir las preferencias.
         */
        const val FICHERO = "sesion"
    }
}
