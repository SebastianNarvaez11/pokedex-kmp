package com.sebastiannarvaez.pokedex.core

/**
 * Lo que cambia entre entornos y entre desarrolladores.
 *
 * Es una interfaz porque las claves **no pueden ser constantes en el codigo**:
 * quien clone el repositorio tiene las suyas, y las de produccion no se
 * commitean. Cada app la construye leyendo de donde su plataforma guarda estas
 * cosas.
 */
interface AppConfig {
    val pokeApiBaseUrl: String

    /** El proyecto de Supabase. Vacio si no se ha configurado. */
    val supabaseUrl: String

    /** La clave publica. Nunca la de servicio. */
    val supabaseKey: String

    /**
     * Si la parte de cuenta esta disponible.
     *
     * La app **funciona sin ella**: la pantalla de sesion se esconde y el resto
     * va igual. Asi el curso se puede seguir sin crear nada, y una clave que
     * falta no es una pantalla en blanco sin explicacion.
     */
    val haySupabase: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
}

/** Base comun: la URL de PokeAPI no es secreta y no cambia. */
abstract class BaseAppConfig : AppConfig {
    override val pokeApiBaseUrl: String = "https://pokeapi.co/api/v2/"
}
