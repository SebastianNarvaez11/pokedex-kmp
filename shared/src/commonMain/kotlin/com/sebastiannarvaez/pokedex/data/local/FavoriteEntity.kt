package com.sebastiannarvaez.pokedex.data.local

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
)
