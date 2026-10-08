package com.sebastiannarvaez.pokedex.data

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.testing.asSnapshot
import com.sebastiannarvaez.pokedex.data.local.createDatabase
import com.sebastiannarvaez.pokedex.data.local.getDatabaseBuilder
import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import com.sebastiannarvaez.pokedex.domain.Pokemon
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

/**
 * El repositorio entero, como lo ve la pantalla: un flujo de paginas.
 *
 * `asSnapshot` recorre ese flujo como lo haria la interfaz, con el Pager, el
 * mediador y el PagingSource de Room de verdad. Lo unico de mentira es la red.
 */
@OptIn(ExperimentalCoroutinesApi::class) // UnconfinedTestDispatcher es experimental
class PokemonListRepositoryTest {

    private val db = createDatabase(getDatabaseBuilder())
    private var reloj = 1_000_000L

    @AfterTest
    fun cerrar() = db.close()

    private fun TestScope.repo(falla: Boolean = false) = PokemonListRepository(
        red = PokemonRepository(FakePokeApi(total = 100, falla = falla), TestDispatchers(StandardTestDispatcher(testScheduler))),
        db = db,
        ahora = { reloj },
    )

    /**
     * Pinta el flujo como lo haria una pantalla y espera a un estado.
     *
     * `asSnapshot` no sirve aqui: ante un error del mediador lanza la
     * excepcion y no deja ver la lista que sigue en pantalla. Esto es lo mismo
     * que hace el presentador de iOS: un `PagingDataPresenter` que recoge.
     */
    private suspend fun TestScope.mirar(
        flujo: Flow<PagingData<Pokemon>>,
        hasta: (CombinedLoadStates) -> Boolean,
    ): Pair<List<Pokemon>, CombinedLoadStates> {
        val hilo = UnconfinedTestDispatcher(testScheduler)
        val presentador = object : PagingDataPresenter<Pokemon>(mainContext = hilo) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<Pokemon>) = Unit
        }
        backgroundScope.launch(hilo) { flujo.collectLatest { presentador.collectFrom(it) } }
        val estados = presentador.loadStateFlow.filterNotNull().first(hasta)
        return presentador.snapshot().items to estados
    }

    @Test
    fun laPrimeraVezLaListaLlegaDeLaRedPasandoPorLaBase() = runTest {
        val items = repo().pokemon().asSnapshot()

        assertEquals("pokemon-1", items.first().name)
        // Lo que se ve es lo que se guardo: la base tiene al menos esa pagina.
        assertTrue(db.pokemonDao().count() >= items.size)
    }

    @Test
    fun sinRedSeVeLoGuardadoYElFalloLlegaDelMediador() = runTest {
        repo().pokemon().asSnapshot()
        val guardados = db.pokemonDao().count()
        // La copia ha caducado: al abrir se intentara refrescar, y fallara.
        reloj += PokemonRemoteMediator.CADUCIDAD.inWholeMilliseconds + 1

        val (items, estados) = mirar(repo(falla = true).pokemon()) { it.mediator?.refresh is LoadState.Error }

        // La lista sigue ahi, entera: el fallo no vacio la base.
        assertEquals("pokemon-1", items.first().name)
        assertEquals(guardados, db.pokemonDao().count())
        // Y el error es del mediador (la red), no de la fuente (la base), que
        // leyo bien. Es la diferencia que la pantalla usa para avisar.
        assertIs<LoadState.Error>(estados.mediator?.refresh)
        assertIs<LoadState.NotLoading>(estados.source.refresh)
    }

    @Test
    fun sinRedYSinNadaGuardadoFalla() = runTest {
        // Lo que antes hacia el PagingSource de red: sin datos, el error llega.
        assertFailsWith<IOException> { repo(falla = true).pokemon().asSnapshot() }
    }
}
