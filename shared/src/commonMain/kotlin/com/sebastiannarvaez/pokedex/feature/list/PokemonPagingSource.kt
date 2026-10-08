package com.sebastiannarvaez.pokedex.feature.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sebastiannarvaez.pokedex.data.PokemonRepository
import com.sebastiannarvaez.pokedex.domain.Pokemon
import kotlinx.coroutines.CancellationException

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
                // La lista solo avanza: nunca se pide una pagina anterior. Con
                // `null` Paging sabe que por arriba no hay nada que cargar.
                prevKey = null,
                // Si la pagina viene corta, se acabo la lista: null apaga la
                // peticion de la siguiente y Paging deja de pedir.
                nextKey = if (pokemon.size < limit) null else offset + limit,
            )
        } catch (e: CancellationException) {
            // Que se cancele la carga (el usuario se va de la pantalla) no es un
            // fallo de red: se relanza para no convertirlo en un error de la lista.
            throw e
        } catch (e: Throwable) {
            // Paging convierte esto en LoadState.Error, que la pantalla pinta
            // con su boton de reintentar. No se traga nada.
            LoadResult.Error(e)
        }
    }

    /**
     * Donde empezar al recargar (tirar para refrescar, reintentar tras un
     * fallo): siempre desde el principio. Como la lista solo avanza y no puede
     * cargar hacia atras, empezar a mitad dejaria fuera todo lo anterior.
     */
    override fun getRefreshKey(state: PagingState<Int, Pokemon>): Int? = null
}
