package com.sebastiannarvaez.pokedex.feature.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.sebastiannarvaez.pokedex.core.AppConfig
import kotlinx.coroutines.flow.StateFlow
import org.koin.mp.KoinPlatform

fun authViewModel(owner: ViewModelStoreOwner): AuthViewModel {
    val koin = KoinPlatform.getKoin()
    return ViewModelProvider.create(
        owner = owner,
        factory = viewModelFactory { initializer { koin.get<AuthViewModel>() } },
    )[AuthViewModel::class]
}

@NativeCoroutinesState
val AuthViewModel.authStateForIos: StateFlow<AuthState>
    get() = state

@NativeCoroutinesState
val AuthViewModel.authFormForIos: StateFlow<AuthFormState>
    get() = form

/**
 * Si la app tiene que pedir cuenta.
 *
 * Swift podria leer `AppConfig` del grafo, pero eso obligaria a exponer Koin al
 * otro lado. Una funcion basta, y deja la libreria de inyeccion donde estaba.
 */
fun hayCuentas(): Boolean = KoinPlatform.getKoin().get<AppConfig>().haySupabase

fun accountViewModel(owner: ViewModelStoreOwner): AccountViewModel {
    val koin = KoinPlatform.getKoin()
    return ViewModelProvider.create(
        owner = owner,
        factory = viewModelFactory { initializer { koin.get<AccountViewModel>() } },
    )[AccountViewModel::class]
}

@NativeCoroutinesState
val AccountViewModel.accountStateForIos: StateFlow<AccountState>
    get() = state
