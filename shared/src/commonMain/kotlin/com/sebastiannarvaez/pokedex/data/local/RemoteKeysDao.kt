package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface RemoteKeysDao {

    @Query("SELECT * FROM remote_keys WHERE lista = :lista")
    suspend fun get(lista: String): RemoteKeysEntity?

    /** REPLACE: la fila es siempre la misma y se sobrescribe. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(keys: RemoteKeysEntity)

    @Query("DELETE FROM remote_keys")
    suspend fun clearAll()
}
