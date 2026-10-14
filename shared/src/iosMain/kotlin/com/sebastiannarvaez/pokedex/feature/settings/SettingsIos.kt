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
