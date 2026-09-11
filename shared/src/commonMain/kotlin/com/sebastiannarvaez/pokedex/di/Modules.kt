package com.sebastiannarvaez.pokedex.di

import com.sebastiannarvaez.pokedex.Pokedex
import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.FavoritesRepository
import com.sebastiannarvaez.pokedex.data.SessionRepository
import com.sebastiannarvaez.pokedex.data.SecureStorageTokenStore
import com.sebastiannarvaez.pokedex.data.TokenStore
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import com.sebastiannarvaez.pokedex.data.local.PokedexDatabase
import com.sebastiannarvaez.pokedex.data.local.createDatabase
import com.sebastiannarvaez.pokedex.data.network.KtorPokeApi
import com.sebastiannarvaez.pokedex.data.network.PokeApi
import com.sebastiannarvaez.pokedex.data.network.KtorSupabaseAccountApi
import com.sebastiannarvaez.pokedex.data.network.KtorSupabaseAuthApi
import com.sebastiannarvaez.pokedex.data.network.ProveedorDeTokens
import com.sebastiannarvaez.pokedex.data.network.SupabaseAccountApi
import com.sebastiannarvaez.pokedex.data.network.createSupabaseAccountClient
import com.sebastiannarvaez.pokedex.data.network.SupabaseAuthApi
import com.sebastiannarvaez.pokedex.data.network.createHttpClient
import com.sebastiannarvaez.pokedex.data.network.createSupabaseClient
import com.sebastiannarvaez.pokedex.core.PlatformAppDispatchers
import com.sebastiannarvaez.pokedex.currentPlatform
import com.sebastiannarvaez.pokedex.feature.auth.AuthViewModel
import com.sebastiannarvaez.pokedex.feature.detail.PokemonDetailViewModel
import com.sebastiannarvaez.pokedex.feature.favorites.FavoritesViewModel
import com.sebastiannarvaez.pokedex.feature.home.HomeViewModel
import com.sebastiannarvaez.pokedex.feature.list.PokemonListViewModel
import com.sebastiannarvaez.pokedex.feature.settings.SettingsViewModel
import com.sebastiannarvaez.pokedex.feature.search.PokemonSearchViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * El grafo que comparten las dos apps.
 *
 * Sin esto, cada interfaz tendria que montar la misma cadena de dependencias
 * por su cuenta: una en Kotlin y otra en Swift, describiendo lo mismo dos
 * veces y desincronizandose a la primera de cambio.
 */
/** El nombre que distingue al cliente de Supabase del de PokeAPI. */
val SUPABASE = org.koin.core.qualifier.named("supabase")

/** El cliente que si lleva la sesion del usuario. */
val SUPABASE_CUENTA = org.koin.core.qualifier.named("supabase-cuenta")

val pokedexModule: Module = module {
    single<AppDispatchers> { PlatformAppDispatchers() }
    single { currentPlatform() }
    single { Pokedex(get()) }

    single { createHttpClient(get()) }
    single<PokeApi> { KtorPokeApi(get()) }
    single { PokemonRepository(get(), get()) }

    single { createDatabase(get()) }
    single { get<PokedexDatabase>().favoriteDao() }
    single { FavoritesRepository(get(), get()) }
    single { SettingsRepository(get()) }

    // El cliente de Supabase lleva `named`: hay dos HttpClient en el grafo y,
    // sin nombre, el segundo sobrescribe al primero sin avisar.
    single(SUPABASE) { createSupabaseClient(get()) }
    single<SupabaseAuthApi> { KtorSupabaseAuthApi(get(SUPABASE)) }
    // El almacen lo pone cada plataforma; lo que se guarda y en que formato,
    // este adaptador, que es comun.
    single<TokenStore> { SecureStorageTokenStore(get(), get()) }
    single { SessionRepository(get(), get(), get()) }

    // El `get()` de dentro se resuelve al hacer la peticion, no al montar el
    // grafo: es lo que rompe el ciclo cliente -> repositorio -> cliente.
    single(SUPABASE_CUENTA) {
        createSupabaseAccountClient(
            get(),
            object : ProveedorDeTokens {
                override suspend fun vigentes() = get<SessionRepository>().tokensVigentes()
                override suspend fun refrescar() = get<SessionRepository>().refrescarAhora()
            },
        )
    }
    single<SupabaseAccountApi> { KtorSupabaseAccountApi(get(SUPABASE_CUENTA)) }

    // viewModelOf y no factory: Koin registra el ViewModel con el ciclo de
    // vida que espera cada plataforma, y en Android lo entrega viewModel().
    viewModelOf(::HomeViewModel)
    viewModelOf(::PokemonListViewModel)
    viewModelOf(::PokemonSearchViewModel)
    viewModelOf(::FavoritesViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AuthViewModel)

    // Con parametro: el identificador no lo sabe el grafo, lo trae la
    // navegacion. `viewModel { }` y no `viewModelOf`, que solo sirve cuando
    // todas las dependencias salen del grafo.
    viewModel { (id: Int) -> PokemonDetailViewModel(get(), id) }
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
