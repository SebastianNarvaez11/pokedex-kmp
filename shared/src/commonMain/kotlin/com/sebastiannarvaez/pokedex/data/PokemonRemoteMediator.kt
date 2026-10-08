package com.sebastiannarvaez.pokedex.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room3.withWriteTransaction
import com.sebastiannarvaez.pokedex.data.local.LISTA_POKEMON
import com.sebastiannarvaez.pokedex.data.local.PokedexDatabase
import com.sebastiannarvaez.pokedex.data.local.PokemonEntity
import com.sebastiannarvaez.pokedex.data.local.RemoteKeysEntity
import com.sebastiannarvaez.pokedex.data.local.aEntidad
import kotlinx.coroutines.CancellationException

/**
 * Llena la base desde la red. **No** le da nada a la pantalla.
 *
 * La pantalla lee solo de la base (el `PagingSource` de Room). Cuando esa
 * lectura llega al final de lo guardado, Paging llama a este mediador para
 * que traiga la pagina siguiente y la escriba en la tabla; Room avisa del
 * cambio y la lista se repinta sola. Asi la base es la unica fuente de verdad
 * de la pantalla, y sin red se sigue viendo lo que ya se guardo.
 *
 * Es el patron que recomienda Google para «red y base de datos»:
 * https://developer.android.com/topic/libraries/architecture/paging/v3-network-db
 */
@OptIn(ExperimentalPagingApi::class)
internal class PokemonRemoteMediator(
    private val red: PokemonRepository,
    private val db: PokedexDatabase,
    /** El reloj, inyectado como en los favoritos: los tests fijan la hora. */
    private val ahora: () -> Long,
) : RemoteMediator<Int, PokemonEntity>() {

    private val pokemonDao = db.pokemonDao()
    private val clavesDao = db.remoteKeysDao()

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PokemonEntity>): MediatorResult {
        val claves = clavesDao.get(LISTA_POKEMON)

        // Donde empieza la pagina que hay que traer.
        val offset = when (loadType) {
            // Refrescar es empezar de cero.
            LoadType.REFRESH -> 0
            // La lista solo avanza: por arriba no hay nada que cargar.
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                // Sin claves, el primer refresco aun no ha escrito nada: no es
                // el final, Paging volvera a llamar cuando lo haya.
                if (claves == null) return MediatorResult.Success(endOfPaginationReached = false)
                // Con claves y sin siguiente, la lista se acabo.
                claves.nextOffset ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        val limit = state.config.pageSize
        return try {
            val pagina = red.page(limit = limit, offset = offset)
            // Si la pagina viene corta, no hay mas: igual que en el PagingSource.
            val fin = pagina.size < limit

            // Todo o nada: si algo falla a mitad, la base queda como estaba.
            // Sin la transaccion, un refresco podria borrar la lista y no
            // llegar a escribir la nueva, y la pantalla se quedaria vacia.
            db.withWriteTransaction {
                if (loadType == LoadType.REFRESH) {
                    pokemonDao.clearAll()
                    clavesDao.clearAll()
                }
                pokemonDao.insertAll(pagina.mapIndexed { i, p -> p.aEntidad(position = offset + i) })
                clavesDao.save(
                    RemoteKeysEntity(
                        lista = LISTA_POKEMON,
                        nextOffset = if (fin) null else offset + limit,
                        // La edad de la copia es la de su primera pagina: al
                        // ampliar se conserva la fecha del ultimo refresco.
                        actualizadoEn = if (loadType == LoadType.REFRESH) ahora() else claves?.actualizadoEn ?: ahora(),
                    ),
                )
            }
            MediatorResult.Success(endOfPaginationReached = fin)
        } catch (e: CancellationException) {
            // Cancelar no es fallar: se relanza, como en el PagingSource.
            throw e
        } catch (e: Throwable) {
            // Lo guardado no se toca. Paging lo convierte en un LoadState.Error
            // del mediador, y la pantalla decide como contarlo.
            MediatorResult.Error(e)
        }
    }

}
