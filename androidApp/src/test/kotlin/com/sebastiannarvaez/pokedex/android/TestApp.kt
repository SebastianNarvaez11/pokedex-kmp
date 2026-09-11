package com.sebastiannarvaez.pokedex.android

import android.app.Application

/**
 * La `Application` que usan los tests.
 *
 * Robolectric arranca **la de verdad**, y la de la Pokedex arranca Koin. Como
 * cada test crea su propio entorno, el segundo fallaba con:
 *
 * ```
 * org.koin.core.error.KoinApplicationAlreadyStartedException:
 * A Koin Application has already been started
 * ```
 *
 * Esta vacia a proposito. Los tests que prueban una pantalla sin ViewModel no
 * necesitan el grafo; los que lo necesiten, montan el suyo y lo paran al
 * terminar.
 */
class TestApp : Application()
