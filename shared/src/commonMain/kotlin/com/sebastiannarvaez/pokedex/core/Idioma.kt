package com.sebastiannarvaez.pokedex.core

/**
 * Los idiomas que quiere el usuario, en orden de preferencia.
 *
 * PokeAPI devuelve el mismo texto en once idiomas dentro del mismo array y **no
 * tiene endpoint por idioma**, asi que el filtro es cosa nuestra. Hasta ahora
 * estaba fijado a espanol: quien tuviera el telefono en ingles veia la ficha en
 * espanol de todas formas.
 *
 * Devuelve una lista y no un idioma porque el sistema tampoco tiene uno: quien
 * configura «catalan, espanol, ingles» espera que se intente en ese orden.
 * Siempre termina en `en`, que es el unico que PokeAPI garantiza.
 */
expect fun idiomasPreferidos(): List<String>

/** El codigo corto, que es lo que usa PokeAPI: `es`, no `es-ES`. */
internal fun String.codigoCorto(): String = substringBefore('-').substringBefore('_').lowercase()
