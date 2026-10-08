package com.sebastiannarvaez.pokedex.data.local

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * La tabla de la lista y la de las claves, en una base en memoria.
 *
 * En `jvmTest`, como `FavoriteDaoTest`, por la misma razon: en la JVM Room
 * monta la base en memoria sin rutas de fichero ni emulador.
 */
class PokemonDaoTest {

    private val db = createDatabase(getDatabaseBuilder())
    private val dao = db.pokemonDao()
    private val claves = db.remoteKeysDao()

    @AfterTest
    fun cerrar() = db.close()

    private fun fila(id: Int, posicion: Int) =
        PokemonEntity(id = id, name = "pokemon-$id", types = "GRASS,POISON", position = posicion)

    @Test
    fun lasPaginasSalenEnElOrdenDeLaLista() = runTest {
        // Se insertan desordenadas a proposito: manda la columna `position`.
        dao.insertAll(listOf(fila(3, 2), fila(1, 0), fila(2, 1)))

        val pager = TestPager(PagingConfig(pageSize = 2, enablePlaceholders = false), dao.pagingSource())
        val pagina = pager.refresh() as PagingSource.LoadResult.Page

        assertEquals(listOf(1, 2, 3), pagina.data.map { it.id })
    }

    @Test
    fun losTiposVanYVuelvenComoTexto() {
        val pokemon = Pokemon(1, "bulbasaur", listOf(PokemonType.GRASS, PokemonType.POISON))

        val entidad = pokemon.aEntidad(position = 0)

        assertEquals("GRASS,POISON", entidad.types)
        assertEquals(pokemon, entidad.aDominio())
    }

    @Test
    fun clearAllVaciaLaTabla() = runTest {
        dao.insertAll(listOf(fila(1, 0), fila(2, 1)))

        dao.clearAll()

        assertEquals(0, dao.count())
    }

    @Test
    fun lasClavesSonUnaSolaFilaQueSeSobrescribe() = runTest {
        assertNull(claves.get(LISTA_POKEMON), "sin descargar nada no hay claves")

        claves.save(RemoteKeysEntity(LISTA_POKEMON, nextOffset = 20, actualizadoEn = 1_000))
        claves.save(RemoteKeysEntity(LISTA_POKEMON, nextOffset = 40, actualizadoEn = 1_000))

        assertEquals(40, claves.get(LISTA_POKEMON)?.nextOffset)
    }
}
