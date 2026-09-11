package com.sebastiannarvaez.pokedex.feature.favorites

import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.data.FavoritesRepository
import com.sebastiannarvaez.pokedex.data.local.createDatabase
import com.sebastiannarvaez.pokedex.data.local.getDatabaseBuilder
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
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

class FavoritesTest {

    private val db = createDatabase(getDatabaseBuilder())
    private var reloj = 1_000L

    @BeforeTest
    fun instalarElHiloPrincipal() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun repo(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        FavoritesRepository(db.favoriteDao(), TestDispatchers(StandardTestDispatcher(scheduler))) { reloj }

    @Test
    fun marcarYDesmarcarDevuelveElEstadoNuevo() = runTest {
        val repo = repo(testScheduler)

        assertTrue(repo.toggle(25, "pikachu"), "marcar deberia devolver true")
        assertFalse(repo.toggle(25, "pikachu"), "desmarcar deberia devolver false")
    }

    @Test
    fun losIdentificadoresLleganComoConjunto() = runTest {
        val repo = repo(testScheduler)

        repo.observeFavoriteIds().test {
            assertEquals(emptySet(), awaitItem())
            repo.toggle(25, "pikachu")
            assertEquals(setOf(25), awaitItem())
            repo.toggle(1, "bulbasaur")
            assertEquals(setOf(25, 1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elRelojInyectadoOrdenaLaLista() = runTest {
        val repo = repo(testScheduler)

        reloj = 1_000; repo.toggle(1, "bulbasaur")
        reloj = 3_000; repo.toggle(25, "pikachu")
        reloj = 2_000; repo.toggle(4, "charmander")

        repo.observeFavorites().test {
            assertEquals(listOf("pikachu", "charmander", "bulbasaur"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elFavoritoSirveSinRed() = runTest {
        val repo = repo(testScheduler)
        repo.toggle(25, "pikachu")

        repo.observeFavorites().test {
            val favorito = awaitItem().single()
            // Nombre e ilustracion sin pedir nada: abrir favoritos en el metro
            // tiene que funcionar.
            assertEquals("pikachu", favorito.name)
            assertTrue(favorito.artworkUrl.endsWith("/25.png"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
