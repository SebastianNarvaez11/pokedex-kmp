package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * En Room 3 **todo es corrutina**.
 *
 * Los metodos que leen una vez son `suspend`; los que observan devuelven
 * `Flow`. No hay nada sincrono, y eso no es un capricho: en Kotlin/Native no
 * existe el equivalente de bloquear el hilo principal y salir vivo.
 */
@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    /** Solo los identificadores: es lo que la lista necesita para pintar el corazon. */
    @Query("SELECT pokemonId FROM favorite")
    fun observeIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE pokemonId = :pokemonId")
    suspend fun remove(pokemonId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE pokemonId = :pokemonId)")
    suspend fun isFavorite(pokemonId: Int): Boolean

    @Query("DELETE FROM favorite")
    suspend fun clear()
}
