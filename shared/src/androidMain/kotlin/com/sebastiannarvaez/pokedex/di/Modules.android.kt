package com.sebastiannarvaez.pokedex.di

import com.sebastiannarvaez.pokedex.data.local.createSettingsStore
import com.sebastiannarvaez.pokedex.data.local.getDatabaseBuilder
import com.sebastiannarvaez.pokedex.data.local.settingsPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder(androidContext()) }
    // Una sola instancia por fichero: dos DataStore sobre el mismo fichero
    // lanzan excepcion. Por eso es `single` y nunca se crea en una pantalla.
    single { createSettingsStore(settingsPath(androidContext())) }
}
