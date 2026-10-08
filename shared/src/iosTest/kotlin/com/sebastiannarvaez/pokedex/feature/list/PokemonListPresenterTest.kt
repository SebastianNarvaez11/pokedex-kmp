package com.sebastiannarvaez.pokedex.feature.list

import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.data.PokemonListRepository
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.data.local.PokedexDatabase
import com.sebastiannarvaez.pokedex.data.local.PokedexDatabaseConstructor
import com.sebastiannarvaez.pokedex.data.local.createDatabase
import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Vive en `iosTest` porque el presentador solo existe en iOS: es el sustituto
 * de lo que en Android hace `paging-compose`.
 */
@OptIn(ExperimentalCoroutinesApi::class) // UnconfinedTestDispatcher es experimental
class PokemonListPresenterTest {

    // Una base en memoria, tambien en iOS: la lista ya no sale de la red sino
    // de aqui. Sin nombre de fichero no hace falta ninguna ruta.
    private val db: PokedexDatabase = createDatabase(
        Room.inMemoryDatabaseBuilder<PokedexDatabase>(factory = PokedexDatabaseConstructor::initialize),
    )
    private var reloj = 1_000_000L

    /** Lo que cada test enciende, para apagarlo entero al terminar. */
    private val presentadores = mutableListOf<PokemonListPresenter>()
    private val viewModels = mutableListOf<PokemonListViewModel>()

    /**
     * El hilo principal, de prueba.
     *
     * `cachedIn(viewModelScope)` reparte las paginas desde `Dispatchers.Main`.
     * Mientras la lista salia de la red no importaba; ahora las paginas llegan
     * desde los hilos de Room, y el salto de vuelta a `Main` se queda en la
     * cola del hilo principal, que el test tiene ocupado esperando: el test
     * muere por tiempo («No value produced in 3s») sin decir por que.
     */
    @BeforeTest
    fun instalarElHiloPrincipal() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun cerrar() {
        db.close()
        Dispatchers.resetMain()
    }

    /**
     * Apaga lo que el test encendio y **espera** a que acabe.
     *
     * Cerrar el presentador no basta: el ViewModel sigue vivo y Room sigue
     * avisandole desde sus hilos, que saltan a `Main`. Si `cerrar()` cierra la
     * base o restaura `Main` mientras tanto, el test falla de vez en cuando
     * («Dispatchers.Main is used concurrently with setting it», «Database is
     * closed» o un cierre del proceso). Por eso se cancela el ambito del
     * ViewModel (lo mismo que hace el sistema al destruirlo) y se espera con
     * `join()` antes de salir del test.
     */
    private suspend fun apagar() {
        presentadores.forEach { it.close() }
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.forEach { it.viewModelScope.coroutineContext.job.join() }
    }

    private fun presentador(total: Int, falla: Boolean, scheduler: TestCoroutineScheduler): PokemonListPresenter {
        // Unconfined y no Standard: el presentador de Paging encadena varias
        // corrutinas antes de publicar el primer estado, y con el dispatcher
        // estandar hay que adelantar el reloj a mano en cada eslabon. Con el
        // no confinado, cada paso corre en cuanto puede.
        val d = TestDispatchers(UnconfinedTestDispatcher(scheduler))
        val red = PokemonRepository(FakePokeApi(total, falla), d)
        val lista = PokemonListRepository(red, db) { reloj }
        val viewModel = PokemonListViewModel(lista).also { viewModels += it }
        return PokemonListPresenter(viewModel, d).also { presentadores += it }
    }

    @Test
    fun laPrimeraPaginaLlegaAlEstado() = runTest {
        val p = presentador(total = 60, falla = false, scheduler = testScheduler)

        p.state.test {
            // El primer valor puede traer ya la pagina: con el dispatcher no
            // confinado, Paging corre antes de que el test se suscriba. Por eso
            // se espera al primer estado con datos en vez de contar emisiones.
            var estado = awaitItem()
            while (estado.items.isEmpty()) estado = awaitItem()

            // No se afirma un numero exacto de elementos: con el dispatcher no
            // confinado, Paging encadena las cargas que puede antes de que el
            // test mire, y ese numero depende del planificador, no de la app.
            // Lo que si es estable es que empiece por el primero y que no haya
            // repetidos.
            assertEquals("pokemon-1", estado.items.first().name)
            assertEquals(estado.items.size, estado.items.map { it.id }.distinct().size, "hay repetidos")
            cancelAndIgnoreRemainingEvents()
        }
        apagar()
    }

    @Test
    fun sinRedNiNadaGuardadoLlegaUnErrorQueSePuedeReintentar() = runTest {
        val p = presentador(total = 60, falla = true, scheduler = testScheduler)

        p.state.test {
            var estado = awaitItem()
            while (estado.error == null) estado = awaitItem()

            assertTrue(estado.items.isEmpty())
            // El `while` de arriba solo termina cuando `error` ya no es `null`,
            // y Kotlin lo sabe: por eso aqui se lee sin `!!` ni `?`.
            assertTrue(estado.error.sePuedeReintentar, "el error deberia ofrecer reintento")
            cancelAndIgnoreRemainingEvents()
        }
        apagar()
    }
}
