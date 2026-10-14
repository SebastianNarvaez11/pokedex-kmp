package com.sebastiannarvaez.pokedex.feature.favorites

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.data.FavoritesRepository
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import com.sebastiannarvaez.pokedex.data.local.createDatabase
import com.sebastiannarvaez.pokedex.data.local.createSettingsStore
import com.sebastiannarvaez.pokedex.data.local.getDatabaseBuilder
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
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

    // Los ajustes de siempre (orden por fecha), en un fichero temporal.
    private fun ajustes() = SettingsRepository(
        createSettingsStore(createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()),
    )

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

    /** Room avisa cuando le da tiempo: se esperan estados hasta el que se busca. */
    private suspend fun ReceiveTurbine<FavoritesUiState>.hasta(condicion: (FavoritesUiState) -> Boolean): FavoritesUiState {
        var estado = awaitItem()
        while (!condicion(estado)) estado = awaitItem()
        return estado
    }

    @Test
    fun quitarDejaElMensajeYDeshacerLoDevuelveASuSitio() = runTest {
        val repo = repo(testScheduler)
        reloj = 1_000; repo.toggle(1, "bulbasaur")
        reloj = 2_000; repo.toggle(4, "charmander")
        reloj = 3_000; repo.toggle(25, "pikachu")
        val vm = FavoritesViewModel(repo, ajustes())

        vm.uiState.test {
            val charmander = hasta { !it.cargando }.favoritos[1]
            reloj = 9_000 // si deshacer usara la hora de ahora, charmander subiria arriba

            vm.quitar(charmander)
            // Room avisa de la fila borrada un instante despues: se espera a las dos cosas.
            val conMensaje = hasta { it.quitado != null && it.favoritos.size == 2 }
            assertEquals(charmander, conMensaje.quitado)
            assertEquals(listOf("pikachu", "bulbasaur"), conMensaje.favoritos.map { it.name })

            vm.deshacer()
            val deshecho = hasta { it.quitado == null && it.favoritos.size == 3 }
            assertEquals(listOf("pikachu", "charmander", "bulbasaur"), deshecho.favoritos.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mensajeMostradoLoConsumeYElFavoritoSigueQuitado() = runTest {
        val repo = repo(testScheduler)
        repo.toggle(25, "pikachu")
        val vm = FavoritesViewModel(repo, ajustes())

        vm.uiState.test {
            val pikachu = hasta { !it.cargando }.favoritos.single()
            vm.quitar(pikachu)
            hasta { it.quitado != null && it.vacia }

            vm.mensajeMostrado()
            val consumido = hasta { it.quitado == null }
            assertTrue(consumido.vacia)
            // Ya no hay nada que deshacer: el gesto llega tarde y no hace nada.
            vm.deshacer()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(repo.quitar(25))
    }

    @Test
    fun elAjusteDeOrdenLosPoneDelPrimeroAlUltimo() = runTest {
        val repo = repo(testScheduler)
        val ajustes = ajustes()
        reloj = 1_000; repo.toggle(4, "charmander")
        reloj = 2_000; repo.toggle(1, "bulbasaur")
        reloj = 3_000; repo.toggle(2, "ivysaur")
        val vm = FavoritesViewModel(repo, ajustes)

        vm.uiState.test {
            // Sin tocar el ajuste: lo mas reciente primero.
            val porFecha = hasta { !it.cargando && it.favoritos.size == 3 }
            assertEquals(listOf(2, 1, 4), porFecha.favoritos.map { it.id })

            // Con el ajuste, se reordena en memoria sin volver a consultar la base.
            ajustes.cambiarOrden(porNumero = true)
            val porNumero = hasta { it.favoritos.first().id == 1 }
            assertEquals(listOf(1, 2, 4), porNumero.favoritos.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun laInsigniaNoSaleConCeroYSeCortaEn99() {
        assertNull(textoInsignia(0), "sin favoritos, sin insignia")
        assertEquals("1", textoInsignia(1))
        assertEquals("99", textoInsignia(99))
        assertEquals("99+", textoInsignia(100), "a partir de 100 no cabe: 99+")
    }

    @Test
    fun laInsigniaSigueALosFavoritos() = runTest {
        val repo = repo(testScheduler)
        val vm = FavoritesViewModel(repo, ajustes())

        vm.insignia.test {
            assertNull(awaitItem(), "al empezar no hay favoritos")
            repo.toggle(25, "pikachu")
            assertEquals("1", awaitItem())
            repo.toggle(4, "charmander")
            assertEquals("2", awaitItem())
            repo.toggle(25, "pikachu")      // desmarcar tambien cuenta
            assertEquals("1", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
