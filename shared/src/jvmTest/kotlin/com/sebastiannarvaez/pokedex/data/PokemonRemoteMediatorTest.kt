package com.sebastiannarvaez.pokedex.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import com.sebastiannarvaez.pokedex.data.local.LISTA_POKEMON
import com.sebastiannarvaez.pokedex.data.local.PokemonEntity
import com.sebastiannarvaez.pokedex.data.local.RemoteKeysEntity
import com.sebastiannarvaez.pokedex.data.local.createDatabase
import com.sebastiannarvaez.pokedex.data.local.getDatabaseBuilder
import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * El mediador, llamado a mano: sin pantalla y sin Pager.
 *
 * Es como lo prueba la guia de Google: se le pide un `load` con un
 * `PagingState` vacio y se mira que dejo en la base. La red es el
 * `FakePokeApi` de siempre; la base, una de verdad en memoria.
 */
@OptIn(ExperimentalPagingApi::class)
class PokemonRemoteMediatorTest {

    private val db = createDatabase(getDatabaseBuilder())
    private var reloj = 1_000_000L

    @AfterTest
    fun cerrar() = db.close()

    /** Un mediador con su propia red. Sin conexion = otro mediador sobre la misma base. */
    private fun TestScope.mediador(total: Int = 60, falla: Boolean = false, api: FakePokeApi = FakePokeApi(total, falla)) =
        PokemonRemoteMediator(
            red = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler))),
            db = db,
            ahora = { reloj },
        )

    /** Lo que Paging le pasa al mediador. Aqui solo importa el tamano de pagina. */
    private val estado = PagingState<Int, PokemonEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20), // las paginas de veinte de la lista
        leadingPlaceholderCount = 0,
    )

    private suspend fun idsGuardados(): List<Int> {
        val pagina = db.pokemonDao().pagingSource().load(
            androidx.paging.PagingSource.LoadParams.Refresh(key = null, loadSize = 1000, placeholdersEnabled = false),
        ) as androidx.paging.PagingSource.LoadResult.Page
        return pagina.data.map { it.id }
    }

    @Test
    fun refrescarLlenaLaBase() = runTest {
        val resultado = mediador().load(LoadType.REFRESH, estado)

        assertIs<MediatorResult.Success>(resultado)
        assertEquals(false, resultado.endOfPaginationReached)
        assertEquals((1..20).toList(), idsGuardados())
        val claves = db.remoteKeysDao().get(LISTA_POKEMON)
        assertEquals(20, claves?.nextOffset)
        assertEquals(reloj, claves?.actualizadoEn)
    }

    @Test
    fun ampliarAnadeLaPaginaSiguiente() = runTest {
        val m = mediador()
        m.load(LoadType.REFRESH, estado)

        m.load(LoadType.APPEND, estado)

        // Del 1 al 40, en orden y sin huecos: la segunda pagina va detras.
        assertEquals((1..40).toList(), idsGuardados())
        assertEquals(40, db.remoteKeysDao().get(LISTA_POKEMON)?.nextOffset)
    }

    @Test
    fun laListaSeAcabaConLaPaginaCorta() = runTest {
        val api = FakePokeApi(total = 25)
        val m = mediador(api = api)
        m.load(LoadType.REFRESH, estado)

        val ultima = m.load(LoadType.APPEND, estado)

        assertIs<MediatorResult.Success>(ultima)
        assertEquals(true, ultima.endOfPaginationReached)
        assertNull(db.remoteKeysDao().get(LISTA_POKEMON)?.nextOffset)

        // Una vez acabada, ampliar ya no gasta red.
        val llamadas = api.llamadasAlListado
        val otra = m.load(LoadType.APPEND, estado)
        assertEquals(true, (otra as MediatorResult.Success).endOfPaginationReached)
        assertEquals(llamadas, api.llamadasAlListado)
    }

    @Test
    fun sinRedLoGuardadoSeConserva() = runTest {
        mediador().load(LoadType.REFRESH, estado)

        val sinRed = mediador(falla = true)
        val alAmpliar = sinRed.load(LoadType.APPEND, estado)
        val alRefrescar = sinRed.load(LoadType.REFRESH, estado)

        // Los dos fallan, y los dos lo dicen: no se tragan el error.
        assertIs<MediatorResult.Error>(alAmpliar)
        assertIs<MediatorResult.Error>(alRefrescar)
        // Y ninguno toca la base: el refresco fallido NO la vacia.
        assertEquals((1..20).toList(), idsGuardados())
        assertEquals(20, db.remoteKeysDao().get(LISTA_POKEMON)?.nextOffset)
    }

    @Test
    fun refrescarBorraLoViejoYEmpiezaDeCero() = runTest {
        // Una copia vieja: un Pokemon que ya no esta y claves por la pagina 3.
        db.pokemonDao().insertAll(listOf(PokemonEntity(id = 999, name = "viejo", types = "", position = 0)))
        db.remoteKeysDao().save(RemoteKeysEntity(LISTA_POKEMON, nextOffset = 60, actualizadoEn = 0))

        mediador().load(LoadType.REFRESH, estado)

        assertEquals((1..20).toList(), idsGuardados())
        assertEquals(20, db.remoteKeysDao().get(LISTA_POKEMON)?.nextOffset)
    }
}
