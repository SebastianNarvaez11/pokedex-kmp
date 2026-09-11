package com.sebastiannarvaez.pokedex.core

/**
 * Lo que cambia entre entornos, como dependencia.
 *
 * Hoy solo hay una URL, y podria ser una constante. Se monta asi desde el
 * principio porque cuando lleguen las claves de Supabase ya no podran ser
 * constantes: tendran que venir de fuera del codigo, y para entonces el sitio
 * donde ponerlas ya existira.
 */
interface AppConfig {
    val pokeApiBaseUrl: String
}

internal class DefaultAppConfig : AppConfig {
    override val pokeApiBaseUrl: String = "https://pokeapi.co/api/v2/"
}
