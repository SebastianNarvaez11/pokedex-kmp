package com.sebastiannarvaez.pokedex.di

import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.core.BaseAppConfig
import com.sebastiannarvaez.pokedex.data.network.SupabaseAccountApi
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertNotSame
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class OrdenTest {
    @Test
    fun elClienteDeSupabaseNoPisaAlDePokeApi() {
        // `AppConfig` lo pone cada app al arrancar: aquí, una de mentira.
        val config = object : BaseAppConfig() {
            override val supabaseUrl = "https://proyecto.supabase.co"
            override val supabaseKey = "clave-publica"
        }
        val koin = startKoin {
            modules(pokedexModule, platformModule, module { single<AppConfig> { config } })
        }.koin
        try {
            assertNotSame(koin.get<HttpClient>(), koin.get<HttpClient>(SUPABASE))
        } finally {
            stopKoin()
        }
    }

    @Test
    fun laApiDeCuentaSeConstruye() {
        val config = object : BaseAppConfig() {
            override val supabaseUrl = "https://proyecto.supabase.co"
            override val supabaseKey = "clave-publica"
        }
        val koin = startKoin {
            modules(pokedexModule, platformModule, module { single<AppConfig> { config } })
        }.koin
        try {
            koin.get<SupabaseAccountApi>()
            assertNotSame(koin.get<HttpClient>(), koin.get<HttpClient>(SUPABASE_CUENTA))
        } finally {
            stopKoin()
        }
    }
}
