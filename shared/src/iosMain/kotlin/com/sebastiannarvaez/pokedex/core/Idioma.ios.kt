package com.sebastiannarvaez.pokedex.core

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun idiomasPreferidos(): List<String> {
    // `preferredLanguages` respeta el orden que el usuario puso en Ajustes, e
    // incluye el idioma por app si lo cambio desde ahi.
    val codigos = NSLocale.preferredLanguages
        .filterIsInstance<String>()
        .map { it.codigoCorto() }
    return (codigos + "en").distinct()
}
