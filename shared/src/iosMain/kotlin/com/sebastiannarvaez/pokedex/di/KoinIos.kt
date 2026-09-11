package com.sebastiannarvaez.pokedex.di

/**
 * El arranque de Koin para iOS.
 *
 * Swift nunca ve Koin: solo llama a esto una vez, al abrirse la app. Asi la
 * interfaz de iOS no depende de la libreria de inyeccion que use el nucleo.
 *
 * Ojo con el nombre al otro lado: `init` es una palabra reservada de
 * Objective-C, asi que `initKoin` llegaria a Swift como `doInitKoin`. Por eso
 * esta funcion se llama distinto.
 */
fun startKoinIos() {
    initKoin()
}
