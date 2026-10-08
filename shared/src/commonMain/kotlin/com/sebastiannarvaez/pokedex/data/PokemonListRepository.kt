package com.sebastiannarvaez.pokedex.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.sebastiannarvaez.pokedex.data.local.PokedexDatabase
import com.sebastiannarvaez.pokedex.data.local.aDominio
import com.sebastiannarvaez.pokedex.domain.Pokemon
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * La lista de Pokemon, con sus dos fuentes: la base y la red.
 *
 * Las dos no estan al mismo nivel. La pantalla lee **solo** de la base, que es
 * la fuente de verdad; la red (`PokemonRepository`) solo sirve para llenarla,
 * y eso lo hace el mediador. Por eso, sin conexion, la lista sigue ahi: lo
 * que llega a la pantalla nunca vino directamente de la red.
 */
@OptIn(ExperimentalTime::class)
internal class PokemonListRepository(
    private val red: PokemonRepository,
    private val db: PokedexDatabase,
    private val ahora: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    @OptIn(ExperimentalPagingApi::class) // RemoteMediator sigue marcado como experimental
    fun pokemon(): Flow<PagingData<Pokemon>> = Pager(
        config = PagingConfig(
            // Veinte por pagina: cada una cuesta una peticion de listado mas
            // una por Pokemon, asi que subirlo multiplica el trafico.
            pageSize = PAGE_SIZE,
            // Cuantos quedan por delante antes de pedir la siguiente pagina.
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        // Quien llena la base cuando se acaba lo guardado.
        remoteMediator = PokemonRemoteMediator(red, db, ahora),
        // De donde lee la pantalla: siempre de la base. Una funcion y no un
        // objeto, porque Room crea un PagingSource nuevo cada vez que la tabla
        // cambia y el anterior queda invalidado.
        pagingSourceFactory = { db.pokemonDao().pagingSource() },
    ).flow.map { pagina -> pagina.map { it.aDominio() } }

    companion object {
        const val PAGE_SIZE = 20
    }
}
