package com.sebastiannarvaez.pokedex

/**
 * El nombre y la version del sistema donde corre este codigo.
 *
 * Es una interfaz y no una `expect class` a proposito: JetBrains desaconseja
 * las clases `expect` donde basta una interfaz, porque una interfaz se puede
 * falsear en los tests y una `expect class` no.
 */
interface Platform {
    val name: String
}

/** Cada plataforma dice quien es. Lo unico que hace falta declarar dos veces. */
expect fun currentPlatform(): Platform
