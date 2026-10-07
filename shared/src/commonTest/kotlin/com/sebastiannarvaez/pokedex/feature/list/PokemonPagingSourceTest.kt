package com.sebastiannarvaez.pokedex.feature.list

import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import androidx.paging.PagingConfig
import androidx.paging.Pager
import com.sebastiannarvaez.pokedex.core.aAppError
import com.sebastiannarvaez.pokedex.core.esReintentable
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class PokemonPagingSourceTest {

    private fun fuente(total: Int, falla: Boolean = false, scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        PokemonPagingSource(PokemonRepository(FakePokeApi(total, falla), TestDispatchers(StandardTestDispatcher(scheduler))))

    @Test
    fun elDesplazamientoAvanzaPaginaAPagina() = runTest {
        // La misma configuracion que usa el ViewModel. Si aqui se pusiera otra,
        // el test estaria probando una app que no existe.
        val pager = Pager(
            config = PagingConfig(
                pageSize = PokemonListViewModel.PAGE_SIZE,
                prefetchDistance = 5,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { fuente(total = 100, scheduler = testScheduler) },
        )

        // asSnapshot recorre el flujo como lo haria la interfaz; dentro del
        // bloque se "mira" hasta el elemento 59, el ultimo de la carga inicial
        // (que no es una pagina sino tres: `initialLoadSize` vale por defecto
        // tres veces `pageSize`, 60 elementos).
        val items = pager.flow.asSnapshot { scrollTo(index = 59) }

        assertEquals("pokemon-1", items.first().name)
        // Al acercarse al final de lo cargado (a menos de `prefetchDistance`),
        // Paging ha pedido otra pagina: hay mas de los 60 iniciales. No se
        // afirma un numero exacto, que depende del reparto entre corrutinas.
        assertTrue(items.size > 60, "no se pidio otra pagina: hay ${items.size}")
        assertEquals(items.size, items.map { it.id }.distinct().size, "hay elementos repetidos")
    }

    @Test
    fun laListaSeAcabaCuandoLaPaginaVieneCorta() = runTest {
        val fuente = fuente(total = 25, scheduler = testScheduler)

        // Se piden las paginas a mano: la primera (`Refresh`, sin clave) y la
        // siguiente (`Append`, desde el 20).
        val primera = fuente.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
        val segunda = fuente.load(PagingSource.LoadParams.Append(key = 20, loadSize = 20, placeholdersEnabled = false))

        assertEquals(20, (primera as PagingSource.LoadResult.Page).data.size)
        val ultima = segunda as PagingSource.LoadResult.Page
        assertEquals(5, ultima.data.size)
        // nextKey a null es lo que apaga la peticion de la pagina siguiente.
        assertEquals(null, ultima.nextKey)
    }

    @Test
    fun unFalloDeRedLlegaComoErrorYNoComoExcepcion() = runTest {
        val fuente = fuente(total = 60, falla = true, scheduler = testScheduler)

        val resultado = fuente.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))

        // La pantalla necesita un estado de error con boton de reintentar, no
        // una excepcion que tumbe la corrutina.
        assertTrue(resultado is PagingSource.LoadResult.Error)
        // Y ese fallo de red tiene que poder reintentarse. `assertTrue(... is ...)`
        // ya le dijo a Kotlin que `resultado` es un `Error`, asi que se puede
        // leer su `throwable` sin convertirlo. Si el doble lanzara algo que no
        // es lo que lanza una red caida, esto fallaria.
        val error = resultado.throwable.aAppError()
        assertTrue(error.esReintentable, "no se puede reintentar: $error")
    }
}
