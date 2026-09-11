package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [FavoriteEntity::class],
    version = 2,
    exportSchema = true,
    // La migracion automatica: Room compara los dos esquemas exportados y
    // escribe el ALTER TABLE. Solo puede hacerlo porque `schemas/1.json` esta
    // versionado; si se hubiera borrado, habria que escribirla a mano.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
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
