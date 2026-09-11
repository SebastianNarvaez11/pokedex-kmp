package com.sebastiannarvaez.pokedex.feature.list

import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import androidx.paging.PagingConfig
import androidx.paging.Pager
import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.data.network.PokeApi
import com.sebastiannarvaez.pokedex.data.network.dto.NamedRefDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonDetailDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonPageDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonRefDto
import com.sebastiannarvaez.pokedex.data.network.dto.TypeSlotDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakePokeApi(private val total: Int, private val falla: Boolean = false) : PokeApi {

    override suspend fun page(limit: Int, offset: Int): PokemonPageDto {
        if (falla) error("sin red")
        val desde = offset + 1
        val hasta = minOf(offset + limit, total)
        return PokemonPageDto(
            count = total,
            results = (desde..hasta).map {
                PokemonRefDto(name = "pokemon-$it", url = "https://pokeapi.co/api/v2/pokemon/$it/")
            },
        )
    }

    override suspend fun detail(id: Int) = PokemonDetailDto(
        id = id,
        name = "pokemon-$id",
        types = listOf(TypeSlotDto(slot = 1, type = NamedRefDto("normal"))),
    )
}

private class TestDispatchers(private val d: CoroutineDispatcher) : AppDispatchers {
    override val io = d
    override val default = d
    override val main = d
}

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

        // asSnapshot recorre el flujo como lo haria la interfaz: pide la
        // primera pagina y luego las siguientes segun se van "viendo".
        val items = pager.flow.asSnapshot { scrollTo(index = 25) }

        assertEquals("pokemon-1", items.first().name)
        // Al llegar al 25 ya se pidio la pagina siguiente, y como faltan menos
        // de `prefetchDistance` para el final de lo cargado, tambien la otra.
        assertTrue(items.size >= 40, "se esperaban al menos dos paginas, hubo ${items.size}")
        assertEquals(items.size, items.map { it.id }.distinct().size, "hay elementos repetidos")
    }

    @Test
    fun laListaSeAcabaCuandoLaPaginaVieneCorta() = runTest {
        val fuente = fuente(total = 25, scheduler = testScheduler)

        val primera = fuente.load(PagingSource.LoadParams.Refresh(null, 20, false))
        val segunda = fuente.load(PagingSource.LoadParams.Append(20, 20, false))

        assertEquals(20, (primera as PagingSource.LoadResult.Page).data.size)
        val ultima = segunda as PagingSource.LoadResult.Page
        assertEquals(5, ultima.data.size)
        // nextKey a null es lo que apaga la peticion de la pagina siguiente.
        assertEquals(null, ultima.nextKey)
    }

    @Test
    fun unFalloDeRedLlegaComoErrorYNoComoExcepcion() = runTest {
        val fuente = fuente(total = 60, falla = true, scheduler = testScheduler)

        val resultado = fuente.load(PagingSource.LoadParams.Refresh(null, 20, false))

        // La pantalla necesita un estado de error con boton de reintentar, no
        // una excepcion que tumbe la corrutina.
        assertTrue(resultado is PagingSource.LoadResult.Error)
    }
}
