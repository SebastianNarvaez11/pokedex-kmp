package com.sebastiannarvaez.pokedex.core

import androidx.core.os.LocaleListCompat

actual fun idiomasPreferidos(): List<String> {
    // `LocaleListCompat.getAdjustedDefault()` tiene en cuenta el idioma por app
    // que Android 13 dejo configurar, no solo el del sistema.
    val lista = LocaleListCompat.getAdjustedDefault()
    val codigos = (0 until lista.size()).mapNotNull { lista[it]?.language?.codigoCorto() }
    return (codigos + "en").distinct()
}
