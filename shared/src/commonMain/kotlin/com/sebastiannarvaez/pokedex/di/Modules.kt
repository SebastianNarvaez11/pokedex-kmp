package com.sebastiannarvaez.pokedex.di

import com.sebastiannarvaez.pokedex.Pokedex
import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.core.DefaultAppConfig
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.data.network.KtorPokeApi
import com.sebastiannarvaez.pokedex.data.network.PokeApi
import com.sebastiannarvaez.pokedex.data.network.createHttpClient
import com.sebastiannarvaez.pokedex.core.PlatformAppDispatchers
import com.sebastiannarvaez.pokedex.currentPlatform
import com.sebastiannarvaez.pokedex.feature.home.HomeViewModel
import com.sebastiannarvaez.pokedex.feature.list.PokemonListViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * El grafo que comparten las dos apps.
 *
 * Sin esto, cada interfaz tendria que montar la misma cadena de dependencias
 * por su cuenta: una en Kotlin y otra en Swift, describiendo lo mismo dos
 * veces y desincronizandose a la primera de cambio.
 */
val pokedexModule: Module = module {
    single<AppDispatchers> { PlatformAppDispatchers() }
    single { currentPlatform() }
    single { Pokedex(get()) }

    single<AppConfig> { DefaultAppConfig() }
    single { createHttpClient(get()) }
    single<PokeApi> { KtorPokeApi(get()) }
    single { PokemonRepository(get(), get()) }

    // viewModelOf y no factory: Koin registra el ViewModel con el ciclo de
    // vida que espera cada plataforma, y en Android lo entrega viewModel().
    viewModelOf(::HomeViewModel)
    viewModelOf(::PokemonListViewModel)
}

/**
 * Lo que solo existe en una plataforma. Hoy esta vacio; se llena cuando lleguen
 * el cliente de red y la base de datos, que si necesitan algo del sistema.
 */
expect val platformModule: Module

/**
 * Arranca Koin. La llaman las dos apps, cada una a su manera, y en ningun otro
 * sitio: un grafo arrancado dos veces lanza excepcion.
 */
fun initKoin(extra: KoinApplication.() -> Unit = {}): KoinApplication =
    startKoin {
        extra()
        modules(pokedexModule, platformModule)
    }
