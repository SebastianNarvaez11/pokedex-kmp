package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase

/**
 * En memoria: la JVM solo existe para los tests, y un fichero compartido entre
 * tests es estado que se arrastra de uno a otro.
 */
fun getDatabaseBuilder(): RoomDatabase.Builder<PokedexDatabase> =
    Room.inMemoryDatabaseBuilder<PokedexDatabase>(factory = PokedexDatabaseConstructor::initialize)

/** En un temporal: la JVM solo existe para los tests. */
fun settingsPath(): String =
    java.nio.file.Files.createTempDirectory("pokedex").resolve(SETTINGS_FILE_NAME).toString()
