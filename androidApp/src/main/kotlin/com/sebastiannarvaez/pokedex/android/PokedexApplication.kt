package com.sebastiannarvaez.pokedex.android

import android.app.Application
import com.sebastiannarvaez.pokedex.android.core.AndroidAppConfig
import com.sebastiannarvaez.pokedex.android.daily.crearCanalDiario
import com.sebastiannarvaez.pokedex.core.AppConfig
import com.sebastiannarvaez.pokedex.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Arranca el grafo una sola vez, al crearse el proceso.
 *
 * `androidContext` deja el Context dentro de Koin, que es como las
 * dependencias que lo necesitan —la base de datos, mas adelante— lo reciben
 * sin que nadie lo pase a mano de constructor en constructor.
 */
class PokedexApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@PokedexApplication)
            // La configuracion la aporta la app, no el modulo compartido: las
            // claves viven donde cada plataforma las guarda.
            modules(module { single<AppConfig> { AndroidAppConfig() } })
        }
        // Crear un canal que ya existe no hace nada, asi que se puede llamar
        // en cada arranque sin comprobar si ya estaba.
        crearCanalDiario(this)
    }
}
