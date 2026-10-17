package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * ¿Nos ha lanzado un test de interfaz? Lo dice el argumento `-uiTests`, el
 * mismo que mira `Pruebas.activas` en Swift. Nadie puede lanzar con argumentos
 * la app que se instala desde la App Store, así que fuera de los tests esto es
 * siempre `false`.
 */
private val enUnTestDeInterfaz: Boolean
    get() = NSProcessInfo.processInfo.arguments.contains("-uiTests")

/**
 * En Documents y no en Caches: el sistema puede vaciar Caches cuando le hace
 * falta espacio, y los favoritos del usuario no son cache.
 */
@OptIn(ExperimentalForeignApi::class)
fun getDatabaseBuilder(): RoomDatabase.Builder<PokedexDatabase> {
    // En un test de interfaz, la base vive en memoria: empieza vacía cada vez
    // que se abre la app, y tus favoritos (los del fichero) no se tocan.
    if (enUnTestDeInterfaz) return Room.inMemoryDatabaseBuilder<PokedexDatabase>()

    val documentos: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    ) ?: error("no se pudo resolver el directorio de documentos")

    return Room.databaseBuilder<PokedexDatabase>(
        name = requireNotNull(documentos.path) + "/" + DATABASE_FILE_NAME,
    )
}

/** El fichero de preferencias, junto a la base de datos. */
@OptIn(ExperimentalForeignApi::class)
fun settingsPath(): String {
    val documentos: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    ) ?: error("no se pudo resolver el directorio de documentos")
    // En un test de interfaz, los ajustes van a otro fichero: el test puede
    // poner el tema «Oscuro» sin cambiar el tuyo.
    val nombre = if (enUnTestDeInterfaz) "pruebas-$SETTINGS_FILE_NAME" else SETTINGS_FILE_NAME
    return requireNotNull(documentos.path) + "/" + nombre
}
