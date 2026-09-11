package com.sebastiannarvaez.pokedex.feature.detail

import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import com.sebastiannarvaez.pokedex.domain.StatKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PokemonDetailTest {

    /**
     * `viewModelScope` cuelga de `Dispatchers.Main`, no del planificador del
     * test. Sin instalarlo, `advanceUntilIdle()` no ejecuta nada de lo que el
     * ViewModel lanza, y el estado se queda en «cargando» para siempre: el test
     * falla sin decir por que.
     */
    @BeforeTest
    fun instalarElHiloPrincipal() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun devolverElHiloPrincipal() {
        Dispatchers.resetMain()
    }

    private fun repo(falla: Boolean = false, scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        PokemonRepository(FakePokeApi(total = 10, falla = falla), TestDispatchers(StandardTestDispatcher(scheduler)))

    @Test
    fun lasMedidasSeConviertenAUnidadesDeHumano() = runTest {
        // PokeAPI mide en decimetros y hectogramos, que no usa nadie.
        val detalle = repo(scheduler = testScheduler).detail(1)

        assertEquals(70, detalle.heightCm)
        assertEquals(6900, detalle.weightG)
    }

    @Test
    fun elTextoLlegaEnEspanolYSinSaltosDeCartucho() = runTest {
        val detalle = repo(scheduler = testScheduler).detail(1)

        assertEquals("Pokemon Semilla", detalle.genus)
        // Los saltos de linea y de pagina venian donde cortaba el cartucho
        // original, no donde acaba una frase.
        assertEquals("Una rara semilla le fue plantada al nacer.", detalle.description)
        assertFalse(detalle.description.contains('\n'))
    }

    @Test
    fun unaEstadisticaDesconocidaNoRompeLaFicha() = runTest {
        val detalle = repo(scheduler = testScheduler).detail(1)

        assertEquals(listOf(StatKind.HP, StatKind.ATTACK), detalle.stats.map { it.kind })
        assertEquals(45, detalle.stats.first { it.kind == StatKind.HP }.value)
    }

    @Test
    fun elViewModelPublicaLaFichaYLuegoDejaDeCargar() = runTest {
        val vm = PokemonDetailViewModel(repo(scheduler = testScheduler), pokemonId = 1)

        assertTrue(vm.uiState.value.cargando)

        testScheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertEquals("pokemon-1", estado.detalle?.name)
        assertEquals(null, estado.error)
    }

    @Test
    fun siFallaLaRedElEstadoTraeUnErrorConReintento() = runTest {
        val vm = PokemonDetailViewModel(repo(falla = true, scheduler = testScheduler), pokemonId = 1)

        testScheduler.advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertEquals(null, estado.detalle)
        assertTrue(estado.error?.sePuedeReintentar == true)
    }
}
