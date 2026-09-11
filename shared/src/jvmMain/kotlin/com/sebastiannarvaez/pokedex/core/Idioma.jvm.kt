package com.sebastiannarvaez.pokedex.core

import java.util.Locale

actual fun idiomasPreferidos(): List<String> =
    listOf(Locale.getDefault().language.codigoCorto(), "en").distinct()
