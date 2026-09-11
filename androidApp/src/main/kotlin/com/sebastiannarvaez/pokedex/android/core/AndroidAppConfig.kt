package com.sebastiannarvaez.pokedex.android.core

import com.sebastiannarvaez.pokedex.android.BuildConfig
import com.sebastiannarvaez.pokedex.core.BaseAppConfig

/**
 * La configuracion de Android, leida de `BuildConfig`.
 *
 * Gradle la rellena desde `secrets.properties`, que no se versiona. Los valores
 * acaban en el binario, asi que **esto no oculta nada de un atacante**: una
 * clave publica se puede extraer de cualquier app. Lo que evita es tenerla en
 * el historial de git, que es otro problema y tambien importa.
 */
class AndroidAppConfig : BaseAppConfig() {
    override val supabaseUrl: String = BuildConfig.SUPABASE_URL
    override val supabaseKey: String = BuildConfig.SUPABASE_KEY
}
