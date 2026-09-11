package com.sebastiannarvaez.pokedex.feature.daily

import kotlinx.datetime.LocalDate

/**
 * El Pokemon del dia, a partir de tres enteros.
 *
 * Existe para **no tener que exportar kotlinx-datetime** al framework. Exportar
 * una libreria entera para poder construir un valor engorda el binario, alarga
 * el header y ata la API de Swift a los tipos de esa libreria. Un puente de
 * tres lineas cuesta menos y envejece mejor.
 */
fun pokemonDelDia(anio: Int, mes: Int, dia: Int): Int =
    PokemonOfTheDay.idParaFecha(LocalDate(anio, mes, dia))
