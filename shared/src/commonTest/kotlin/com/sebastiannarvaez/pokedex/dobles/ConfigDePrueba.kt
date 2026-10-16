package com.sebastiannarvaez.pokedex.dobles

import com.sebastiannarvaez.pokedex.core.BaseAppConfig

// La configuración de mentira de todos los tests. La URL de PokeAPI la hereda
// de `BaseAppConfig`; las dos claves llevan un valor cualquiera, y se pueden
// cambiar al construirla: `ConfigDePrueba(supabaseUrl = "")`. `internal`: solo
// la ven los tests de este módulo.
internal class ConfigDePrueba(
    override val supabaseUrl: String = "https://proyecto.supabase.co",
    override val supabaseKey: String = "clave-publica",
) : BaseAppConfig()
