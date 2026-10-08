package com.sebastiannarvaez.pokedex.feature.list

import androidx.room3.Room
import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.data.PokemonListRepository
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.data.local.PokedexDatabase
import com.sebastiannarvaez.pokedex.data.local.PokedexDatabaseConstructor
import com.sebastiannarvaez.pokedex.data.local.createDatabase
import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
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

    @AfterTest
    fun cerrar() = db.close()

    private fun presentador(total: Int, falla: Boolean, scheduler: TestCoroutineScheduler): PokemonListPresenter {
        // Unconfined y no Standard: el presentador de Paging encadena varias
        // corrutinas antes de publicar el primer estado, y con el dispatcher
        // estandar hay que adelantar el reloj a mano en cada eslabon. Con el
        // no confinado, cada paso corre en cuanto puede.
        val d = TestDispatchers(UnconfinedTestDispatcher(scheduler))
        val red = PokemonRepository(FakePokeApi(total, falla), d)
        return PokemonListPresenter(PokemonListViewModel(PokemonListRepository(red, db)), d)
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
            // Lo que si es estable es que empiece por el primero, que llegue al
            // menos una pagina y que no haya repetidos.
            assertTrue(estado.items.size >= 20, "llego menos de una pagina: ${estado.items.size}")
            assertEquals("pokemon-1", estado.items.first().name)
            assertEquals(estado.items.size, estado.items.map { it.id }.distinct().size, "hay repetidos")
            cancelAndIgnoreRemainingEvents()
        }
        p.close()
    }

    @Test
    fun unFalloDeRedLlegaComoErrorQueSePuedeReintentar() = runTest {
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
        p.close()
    }
}
