package com.sebastiannarvaez.pokedex.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/**
 * Lo unico que cambia por plataforma: donde esta el fichero.
 *
 * Es una funcion normal, no un `expect/actual`. La decision de que
 * implementacion usar la toma Koin, que es donde debe tomarse.
 */
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<PokedexDatabase> =
    Room.databaseBuilder<PokedexDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
    )

/** El fichero de preferencias, en el directorio privado de la app. */
fun settingsPath(context: Context): String =
    context.filesDir.resolve(SETTINGS_FILE_NAME).absolutePath
