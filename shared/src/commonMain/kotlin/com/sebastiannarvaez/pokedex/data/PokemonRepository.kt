package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.network.PokeApi
import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Traduce lo que da PokeAPI a lo que la app necesita.
 *
 * El listado de PokeAPI no trae ni tipos ni imagen: solo nombre y URL. Para
 * pintar una tarjeta decente hacen falta los tipos, asi que por cada pagina se
 * piden tambien los detalles. Es un N+1, y se hace con los ojos abiertos: las
 * peticiones van **en paralelo**, la pagina es de veinte, y mas adelante habra
 * cache. Hacerlas en serie multiplicaria por veinte la espera.
 */
internal class PokemonRepository(
    private val api: PokeApi,
    private val dispatchers: AppDispatchers,
) {

    suspend fun page(limit: Int, offset: Int): List<Pokemon> = withContext(dispatchers.io) {
        val pagina = api.page(limit = limit, offset = offset)

        coroutineScope {
            pagina.results
                .mapNotNull { it.id }
                .map { id -> async { pokemon(id) } }
                .awaitAll()
        }
    }

    suspend fun pokemon(id: Int): Pokemon = withContext(dispatchers.io) {
        val detalle = api.detail(id)
        Pokemon(
            id = detalle.id,
            name = detalle.name,
            types = detalle.types
                .sortedBy { it.slot }
                .mapNotNull { PokemonType.deApi(it.type.name) },
        )
    }
}
