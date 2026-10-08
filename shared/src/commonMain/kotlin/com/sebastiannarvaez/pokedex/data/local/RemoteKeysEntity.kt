package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Por donde va la descarga de la lista, y cuando se hizo.
 *
 * Es una sola fila por lista. La lista de PokeAPI solo avanza, asi que basta
 * con recordar **un** numero: el desplazamiento de la pagina siguiente. Es la
 * «clave de pagina» de la documentacion de Google (una tabla con una clave por
 * consulta); la otra variante, una clave por elemento, sirve cuando la API se
 * pide a partir del ultimo elemento visto.
 */
@Entity(tableName = "remote_keys")
data class RemoteKeysEntity(
    /** De que lista son las claves. Hoy solo hay una: `LISTA_POKEMON`. */
    @PrimaryKey val lista: String,
    /** El desplazamiento de la pagina siguiente. `null`: ya no hay mas. */
    val nextOffset: Int?,
    /** Cuando se descargo la primera pagina, en milisegundos. Mide la edad de la copia. */
    val actualizadoEn: Long,
)

/** El nombre de la unica fila de `RemoteKeysEntity`. */
const val LISTA_POKEMON = "pokemon"
