package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

internal const val DATABASE_FILE_NAME = "pokedex.db"

/**
 * El driver se elige una sola vez, aqui.
 *
 * `BundledSQLiteDriver` incrusta SQLite en la app en vez de usar el del
 * sistema: la misma version del motor en Android, en iOS y en la JVM, sin
 * sorpresas de «en mi movil si funciona».
 */
fun createDatabase(builder: RoomDatabase.Builder<PokedexDatabase>): PokedexDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .build()
