package com.sebastiannarvaez.pokedex.android

import android.app.Application
import com.sebastiannarvaez.pokedex.android.daily.crearCanalDiario
import com.sebastiannarvaez.pokedex.di.initKoin
import org.koin.android.ext.koin.androidContext

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
        initKoin { androidContext(this@PokedexApplication) }
        // Crear un canal que ya existe no hace nada, asi que se puede llamar
        // en cada arranque sin comprobar si ya estaba.
        crearCanalDiario(this)
    }
}
