package com.sebastiannarvaez.pokedex.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sebastiannarvaez.pokedex.core.Log
import com.sebastiannarvaez.pokedex.feature.settings.Settings
import com.sebastiannarvaez.pokedex.feature.settings.Tema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Las preferencias, como flujo.
 *
 * DataStore no es SharedPreferences con otro nombre: no tiene `commit()` ni
 * lecturas sincronas, y cada cambio llega como emision. Eso obliga a pensar la
 * pantalla como algo que **observa** su configuracion, no que la consulta.
 */
internal class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<Settings> = store.data
        .catch { causa ->
            // Un fichero corrupto no debe impedir abrir la app: se registra y
            // se sigue con los valores por defecto.
            Log.w("no se pudieron leer las preferencias", causa as? Throwable)
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { prefs ->
            Settings(
                tema = prefs[CLAVE_TEMA]?.let { runCatching { Tema.valueOf(it) }.getOrNull() } ?: Tema.SISTEMA,
                favoritosPorNumero = prefs[CLAVE_ORDEN] ?: false,
            )
        }

    suspend fun cambiarTema(tema: Tema) {
        store.edit { it[CLAVE_TEMA] = tema.name }
    }

    suspend fun cambiarOrden(porNumero: Boolean) {
        store.edit { it[CLAVE_ORDEN] = porNumero }
    }

    private companion object {
        val CLAVE_TEMA = stringPreferencesKey("tema")
        val CLAVE_ORDEN = booleanPreferencesKey("favoritos_por_numero")
    }
}
