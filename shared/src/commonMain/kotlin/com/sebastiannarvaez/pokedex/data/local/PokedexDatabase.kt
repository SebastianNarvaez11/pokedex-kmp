package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = true)
@ConstructedBy(PokedexDatabaseConstructor::class)
abstract class PokedexDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}

/**
 * Lo genera KSP en cada plataforma. El `expect` sin `actual` visible es
 * correcto: por eso hace falta el `@Suppress`, o el IDE marca error en rojo
 * sobre codigo que compila perfectamente.
 */
@Suppress("KotlinNoActualForExpect")
expect object PokedexDatabaseConstructor : RoomDatabaseConstructor<PokedexDatabase> {
    override fun initialize(): PokedexDatabase
}
