package com.sebastiannarvaez.pokedex.core

// Lo que cambia entre entornos y entre desarrolladores. Es una interfaz porque
// las claves no pueden ser constantes en el código: cada app las lee del sitio
// donde su plataforma las guarda.
interface AppConfig {
    // La dirección de PokeAPI: no es secreta y no cambia.
    val pokeApiBaseUrl: String

    // El proyecto de Supabase. Vacío si no se ha configurado.
    val supabaseUrl: String

    // La clave pública. Nunca la de servicio.
    val supabaseKey: String

    // Si la parte de cuenta está disponible. Una propiedad con `get()` se
    // calcula cada vez que se lee. La app funciona sin cuenta: si falta
    // alguno de los dos valores, la pantalla de sesión se esconde y el resto
    // va igual.
    val haySupabase: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
}

// Una base con lo que es igual en todas partes: la dirección de PokeAPI. Las
// apps heredan de ella y rellenan solo las dos claves.
abstract class BaseAppConfig : AppConfig {
    override val pokeApiBaseUrl: String = "https://pokeapi.co/api/v2/"
}
