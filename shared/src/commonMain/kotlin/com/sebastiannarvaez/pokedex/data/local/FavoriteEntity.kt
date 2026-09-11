package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Un favorito, tal y como se guarda en la base.
 *
 * Es una clase aparte del modelo de dominio a proposito: una fila tiene tipos
 * de columna, clave primaria y anotaciones, y el dominio no debe saber de eso.
 * La misma razon por la que los DTO de red tampoco son el dominio.
 *
 * Guarda el nombre ademas del identificador para poder pintar la lista de
 * favoritos **sin red**. Si solo guardara el numero, abrir favoritos en el
 * metro daria una pantalla vacia.
 */
@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val pokemonId: Int,
    val name: String,
    /** Cuando se marco. Sirve para ordenar por lo mas reciente. */
    val addedAt: Long,
    /**
     * El tipo principal, guardado con el favorito.
     *
     * Llego en la version 2 del esquema. Sin el, la lista de favoritos no
     * podia pintar el color sin pedir el detalle de cada uno, y eso convertia
     * una pantalla que funcionaba sin red en una que no.
     *
     * `defaultValue` no es opcional: sin el, Room no sabe que poner en las
     * filas que ya existen y la migracion automatica **no se puede generar**.
     */
    @ColumnInfo(defaultValue = "")
    val primaryType: String = "",
)
