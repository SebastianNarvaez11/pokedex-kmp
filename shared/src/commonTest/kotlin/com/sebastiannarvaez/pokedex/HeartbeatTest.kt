package com.sebastiannarvaez.pokedex

import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.core.AppDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

private class TestDispatchers(private val d: CoroutineDispatcher) : AppDispatchers {
    override val io = d
    override val default = d
    override val main = d
}

class HeartbeatTest {

    /**
     * Tres segundos de reloj, cero segundos de espera.
     *
     * `runTest` sustituye el reloj por uno virtual: el `delay(1.seconds)` del
     * flujo no espera, salta. Un test que comprobara esto con esperas reales
     * tardaria tres segundos y seria inestable en un servidor cargado.
     *
     * Turbine es lo que permite pedir emisiones de una en una. Sin el, para
     * probar un flujo infinito habria que recolectarlo en otra corrutina y
     * cancelarla a mano, que es donde salen los tests que fallan a veces.
     */
    @Test
    fun elLatidoCuentaDeUnoEnUno() = runTest {
        val pokedex = Pokedex(TestDispatchers(StandardTestDispatcher(testScheduler)))

        pokedex.heartbeat().test(timeout = 5.seconds) {
            assertEquals(0, awaitItem())
            assertEquals(1, awaitItem())
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
