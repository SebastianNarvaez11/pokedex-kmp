package com.sebastiannarvaez.pokedex.di

import com.sebastiannarvaez.pokedex.data.InMemorySecureStorage
import com.sebastiannarvaez.pokedex.data.SecureStorage
import com.sebastiannarvaez.pokedex.data.local.createSettingsStore
import com.sebastiannarvaez.pokedex.data.local.getDatabaseBuilder
import com.sebastiannarvaez.pokedex.data.local.settingsPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder() }
    // Una sola instancia por fichero: dos DataStore sobre el mismo fichero
    // lanzan excepcion. Por eso es `single` y nunca se crea en una pantalla.
    single { createSettingsStore(settingsPath()) }
    // El target de JVM solo existe para tests: no hay llavero que valga.
    single<SecureStorage> { InMemorySecureStorage() }
}
