package com.sebastiannarvaez.pokedex.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val SETTINGS_FILE_NAME = "pokedex.preferences_pb"

/**
 * Crea el almacen de preferencias a partir de una ruta.
 *
 * **Una sola instancia por fichero.** Dos `DataStore` sobre el mismo fichero
 * lanzan excepcion, y por eso esto se registra como `single` en Koin y nunca
 * se llama desde una pantalla.
 */
fun createSettingsStore(ruta: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { ruta.toPath() })
