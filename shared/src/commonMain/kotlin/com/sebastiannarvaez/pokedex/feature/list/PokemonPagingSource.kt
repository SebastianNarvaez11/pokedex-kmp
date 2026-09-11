package com.sebastiannarvaez.pokedex.feature.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.domain.Pokemon

/**
 * De donde salen las paginas.
 *
 * Paging no es una comodidad: es lo que evita escribir a mano el contador de
 * desplazamiento, la deteccion del final de la lista, el estado de «cargando
 * mas», los reintentos y la deduplicacion cuando el usuario sube y baja
 * rapido. Todo eso son bugs conocidos, y estan resueltos aqui.
 *
 * PokeAPI pagina por desplazamiento, no por cursor, asi que la clave de cada
 * pagina es simplemente cuantos elementos hay antes.
 */
internal class PokemonPagingSource(
    private val repository: PokemonRepository,
) : PagingSource<Int, Pokemon>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pokemon> {
        val offset = params.key ?: 0
        val limit = params.loadSize

        return try {
            val pokemon = repository.page(limit = limit, offset = offset)
            LoadResult.Page(
                data = pokemon,
                prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0),
                // Si la pagina viene corta, se acabo la lista: null apaga la
                // peticion de la siguiente y Paging deja de pedir.
                nextKey = if (pokemon.size < limit) null else offset + limit,
            )
        } catch (e: Throwable) {
            // Paging convierte esto en LoadState.Error, que la pantalla pinta
            // con su boton de reintentar. No se traga nada.
            LoadResult.Error(e)
        }
    }

    /**
     * Donde recargar cuando el usuario tira para refrescar, para que no salte
     * al principio de la lista.
     */
    override fun getRefreshKey(state: PagingState<Int, Pokemon>): Int? =
        state.anchorPosition?.let { ancla ->
            val pagina = state.closestPageToPosition(ancla)
            pagina?.prevKey?.plus(state.config.pageSize) ?: pagina?.nextKey?.minus(state.config.pageSize)
        }
}
