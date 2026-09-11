package com.sebastiannarvaez.pokedex

/**
 * El nombre y la version del sistema donde corre este codigo.
 *
 * Es una interfaz y no una `expect class` a proposito. JetBrains desaconseja
 * las clases `expect` cuando basta una interfaz, y la razon practica se ve en
 * los tests: de una interfaz se escribe una implementacion falsa en tres
 * lineas, y de una `expect class` no se puede.
 */
interface Platform {
    val name: String
}

/**
 * La unica frontera declarada dos veces en todo el modulo.
 *
 * No se llama desde el codigo de dominio: la llama quien arranca la app, una
 * sola vez, y a partir de ahi la `Platform` viaja como parametro. Cuantas menos
 * llamadas tenga esta funcion, menos codigo queda atado a la plataforma.
 */
expect fun currentPlatform(): Platform
