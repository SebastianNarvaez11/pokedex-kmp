package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType

/**
 * Un Pokemon de la lista, tal y como se guarda en la base.
 *
 * Es la copia local de lo que trajo la red: la pantalla de la lista ya no lee
 * de PokeAPI, lee de esta tabla. Por eso guarda todo lo que la tarjeta pinta
 * (numero, nombre y tipos); la imagen no hace falta, porque sale del numero.
 */
@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /**
     * Los tipos como texto, separados por comas: `"GRASS,POISON"`.
     *
     * SQLite no tiene columnas de tipo lista. Se guarda el nombre de cada
     * `enum`, como ya se hacia con el tipo del favorito, y al leer se
     * traduce de vuelta con `PokemonType.deApi`.
     */
    val types: String,
    /**
     * El puesto en la lista de PokeAPI: 0, 1, 2…
     *
     * La pantalla ordena por esta columna y no por el numero: el orden lo
     * decide el servidor, y la base solo lo recuerda.
     */
    val position: Int,
)

/** De fila a dominio: lo que la pantalla sabe pintar. */
fun PokemonEntity.aDominio(): Pokemon = Pokemon(
    id = id,
    name = name,
    types = types.split(",").mapNotNull { PokemonType.deApi(it) },
)

/** De dominio a fila, con el puesto que ocupa en la lista. */
fun Pokemon.aEntidad(position: Int): PokemonEntity = PokemonEntity(
    id = id,
    name = name,
    types = types.joinToString(",") { it.name },
    position = position,
)
