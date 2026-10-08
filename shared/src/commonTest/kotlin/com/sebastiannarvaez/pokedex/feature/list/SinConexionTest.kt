package com.sebastiannarvaez.pokedex.feature.list

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** La regla del aviso, sin pantalla: solo estados de carga escritos a mano. */
class SinConexionTest {

    private val bien = LoadStates(
        refresh = LoadState.NotLoading(false),
        prepend = LoadState.NotLoading(true),
        append = LoadState.NotLoading(false),
    )

    private fun estados(mediador: LoadStates?) = CombinedLoadStates(
        refresh = mediador?.refresh ?: bien.refresh,
        prepend = bien.prepend,
        append = mediador?.append ?: bien.append,
        source = bien,
        mediator = mediador,
    )

    private val fallo = LoadState.Error(Exception("sin red"))

    @Test
    fun conFilasYLaRedCaidaSeAvisa() {
        assertTrue(estados(bien.copy(refresh = fallo)).mostrandoLoGuardado(hayFilas = true))
        assertTrue(estados(bien.copy(append = fallo)).mostrandoLoGuardado(hayFilas = true))
    }

    @Test
    fun sinFilasNoEsUnAvisoEsElError() {
        // Sin nada guardado no hay nada que «seguir ensenando»: le toca a la
        // pantalla de error de siempre.
        assertFalse(estados(bien.copy(refresh = fallo)).mostrandoLoGuardado(hayFilas = false))
    }

    @Test
    fun conLaRedBienNoSeAvisa() {
        assertFalse(estados(bien).mostrandoLoGuardado(hayFilas = true))
        // Y sin mediador (la lista de antes, solo red), tampoco.
        assertFalse(estados(null).mostrandoLoGuardado(hayFilas = true))
    }
}
