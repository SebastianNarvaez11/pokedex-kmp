package com.sebastiannarvaez.pokedex.feature.search

import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PokemonSearchTest {

    @BeforeTest
    fun instalarElHiloPrincipal() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun devolverElHiloPrincipal() = Dispatchers.resetMain()

    @Test
    fun elIndiceSePideUnaSolaVezAunqueSeBusqueVariasVeces() = runTest {
        val api = FakePokeApi(total = 30)
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        repo.searchIndex()
        repo.searchIndex()
        repo.searchIndex()

        // Una sola peticion: el indice se guarda en memoria. Si esto falla,
        // cada letra escrita se trae mil trescientos nombres otra vez.
        assertEquals(1, api.llamadasAlListado)
    }

    @Test
    fun buscarFiltraPorLoQueContieneElNombre() = runTest {
        val repo = PokemonRepository(FakePokeApi(total = 30), TestDispatchers(StandardTestDispatcher(testScheduler)))
        val vm = PokemonSearchViewModel(repo)

        val estados = mutableListOf<SearchUiState>()
        // `backgroundScope` y no una corrutina suelta: runTest lo cancela al
        // acabar el test. Con `launch` a pelo, el test no termina nunca porque
        // el flujo es infinito.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect { estados.add(it) }
        }

        vm.escribir("pokemon-1")
        testScheduler.advanceUntilIdle()

        val ultimo = estados.last()
        // pokemon-1, pokemon-10..pokemon-19: once nombres contienen «pokemon-1»
        assertTrue(ultimo.resultados.isNotEmpty(), "no encontro nada")
        assertTrue(ultimo.resultados.all { it.name.contains("pokemon-1") })
    }

    @Test
    fun loQueEmpiezaPorLoEscritoVaPrimero() = runTest {
        val repo = PokemonRepository(FakePokeApi(total = 30), TestDispatchers(StandardTestDispatcher(testScheduler)))
        val vm = PokemonSearchViewModel(repo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        vm.escribir("pokemon-2")
        testScheduler.advanceUntilIdle()

        val primero = vm.uiState.value.resultados.first()
        assertTrue(primero.name.startsWith("pokemon-2"), "el primero fue ${primero.name}")
    }

    @Test
    fun conElCampoVacioNoSeBuscaNada() = runTest {
        val api = FakePokeApi(total = 30)
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))
        val vm = PokemonSearchViewModel(repo)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        vm.escribir("   ")
        testScheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.enReposo)
        assertEquals(0, api.llamadasAlListado, "no deberia haber pedido el indice")
    }
}
