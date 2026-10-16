package com.sebastiannarvaez.pokedex.android.core

// BuildConfig lo genera Gradle en este mismo paquete (el `namespace` de
// androidApp). Si tu `namespace` es otro, usa el tuyo en el paquete y aquí.
import com.sebastiannarvaez.pokedex.android.BuildConfig
import com.sebastiannarvaez.pokedex.core.BaseAppConfig

// La configuración de Android: hereda la URL de PokeAPI y rellena las dos
// claves con las constantes que Gradle sacó de `local.properties`.
class AndroidAppConfig : BaseAppConfig() {
    override val supabaseUrl: String = BuildConfig.SUPABASE_URL
    override val supabaseKey: String = BuildConfig.SUPABASE_KEY
}
