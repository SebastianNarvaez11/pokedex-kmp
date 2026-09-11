package com.sebastiannarvaez.pokedex.data.local

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Vive en `jvmTest` y no en `commonTest` porque crear una base de datos en un
 * test de Android exigiria Robolectric, y en el simulador de iOS habria que
 * darle una ruta de fichero. En la JVM, Room la monta en memoria.
 *
 * No se pierde nada importante: el SQL es el mismo en las tres plataformas
 * porque el motor va incrustado, no es el del sistema.
 */
class FavoriteDaoTest {

    private val db = createDatabase(getDatabaseBuilder())
    private val dao = db.favoriteDao()

    @AfterTest
    fun cerrar() = db.close()

    @Test
    fun guardaYRecuperaUnFavorito() = runTest {
        dao.add(FavoriteEntity(pokemonId = 25, name = "pikachu", addedAt = 1_000))

        assertTrue(dao.isFavorite(25))
        assertFalse(dao.isFavorite(26))
    }

    @Test
    fun losMasRecientesVanPrimero() = runTest {
        dao.add(FavoriteEntity(1, "bulbasaur", addedAt = 1_000))
        dao.add(FavoriteEntity(25, "pikachu", addedAt = 3_000))
        dao.add(FavoriteEntity(4, "charmander", addedAt = 2_000))

        dao.observeAll().test {
            assertEquals(listOf("pikachu", "charmander", "bulbasaur"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun marcarDosVecesNoDuplica() = runTest {
        // REPLACE y no IGNORE: si el usuario vuelve a marcarlo, se actualiza la
        // fecha, que es lo que espera al ver la lista ordenada por reciente.
        dao.add(FavoriteEntity(25, "pikachu", addedAt = 1_000))
        dao.add(FavoriteEntity(25, "pikachu", addedAt = 5_000))

        dao.observeAll().test {
            val todos = awaitItem()
            assertEquals(1, todos.size)
            assertEquals(5_000, todos.first().addedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elFlujoAvisaAlQuitarUnFavorito() = runTest {
        dao.add(FavoriteEntity(25, "pikachu", addedAt = 1_000))

        dao.observeIds().test {
            assertEquals(listOf(25), awaitItem())
            dao.remove(25)
            // Room reemite solo: la pantalla no tiene que preguntar.
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
