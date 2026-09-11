package com.sebastiannarvaez.pokedex.feature.settings

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.StateFlow
import org.koin.mp.KoinPlatform

fun settingsViewModel(owner: ViewModelStoreOwner): SettingsViewModel {
    val koin = KoinPlatform.getKoin()
    return ViewModelProvider.create(
        owner = owner,
        factory = viewModelFactory { initializer { koin.get<SettingsViewModel>() } },
    )[SettingsViewModel::class]
}

@NativeCoroutinesState
val SettingsViewModel.settingsForIos: StateFlow<Settings>
    get() = settings

/**
 * El tema como texto.
 *
 * Un `enum` de Kotlin llega a Swift como clase, y comparar instancias desde
 * SwiftUI es incomodo. Con un `String` de por medio, el `Picker` funciona con
 * `String` a secas y la conversion vive en un solo sitio.
 */
fun temaDesdeNombre(nombre: String): Tema =
    Tema.entries.firstOrNull { it.name == nombre } ?: Tema.SISTEMA
