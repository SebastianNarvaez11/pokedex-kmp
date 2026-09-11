package com.sebastiannarvaez.pokedex.di

import kotlin.test.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/**
 * Vive en `jvmTest` y no en `commonTest` porque `verify()` funciona por
 * reflexion, y **Kotlin/Native no la tiene**. En commonTest el compilador
 * responde `Unresolved reference 'verify'` al compilar para el simulador.
 *
 * No se pierde nada: el grafo es el mismo en las tres plataformas, asi que
 * comprobarlo una vez basta.
 */
class ModulesTest {

    /**
     * Comprueba que el grafo se puede construir entero sin arrancar la app.
     *
     * No llama a `startKoin`: arrancar el grafo en un test deja estado global
     * entre tests, y el siguiente falla por razones que no tienen que ver con
     * lo que probaba. `verify` recorre las definiciones y avisa de cualquier
     * dependencia que nadie sepa construir.
     */
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun elGrafoSePuedeConstruirEntero() {
        pokedexModule.verify()
    }
}
