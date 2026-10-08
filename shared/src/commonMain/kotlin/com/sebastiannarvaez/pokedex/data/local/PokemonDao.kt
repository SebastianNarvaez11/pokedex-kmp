package com.sebastiannarvaez.pokedex.data.local

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter

/**
 * La lista de Pokemon guardada.
 *
 * `@DaoReturnTypeConverters` es lo que deja a una consulta devolver un
 * `PagingSource`: el convertidor vive en `room3-paging`. Sin la anotacion,
 * Room no sabe construir ese tipo y KSP falla al compilar.
 */
@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface PokemonDao {

    /**
     * Las paginas, leidas de la base.
     *
     * Room escribe el `PagingSource` entero: cuenta, recorta con LIMIT y
     * OFFSET y, lo mas importante, **avisa solo** cuando la tabla cambia, asi
     * que la lista se repinta en cuanto llega una pagina nueva.
     */
    @Query("SELECT * FROM pokemon ORDER BY position")
    fun pagingSource(): PagingSource<Int, PokemonEntity>

    /** REPLACE: si un Pokemon ya estaba, se sustituye por la version nueva. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pokemon: List<PokemonEntity>)

    @Query("DELETE FROM pokemon")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int
}
