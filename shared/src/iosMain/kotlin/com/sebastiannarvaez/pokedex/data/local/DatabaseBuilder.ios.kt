package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * En Documents y no en Caches: el sistema puede vaciar Caches cuando le hace
 * falta espacio, y los favoritos del usuario no son cache.
 */
@OptIn(ExperimentalForeignApi::class)
fun getDatabaseBuilder(): RoomDatabase.Builder<PokedexDatabase> {
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
    return requireNotNull(documentos.path) + "/" + SETTINGS_FILE_NAME
}
