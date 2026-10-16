package com.sebastiannarvaez.pokedex.feature.daily

import kotlinx.datetime.LocalDate

/**
 * El Pokémon del día a partir de tres enteros.
 * Swift lo ve como `DailyIosKt.pokemonDelDia(anio:mes:dia:)`: la función suelta
 * de un fichero llega a Swift dentro de una clase con el nombre del fichero + `Kt`
 * (la «fachada» de Tipos que cruzan).
 */
fun pokemonDelDia(anio: Int, mes: Int, dia: Int): Int =
    PokemonOfTheDay.idParaFecha(LocalDate(anio, mes, dia))   // construye la fecha aquí, en Kotlin
