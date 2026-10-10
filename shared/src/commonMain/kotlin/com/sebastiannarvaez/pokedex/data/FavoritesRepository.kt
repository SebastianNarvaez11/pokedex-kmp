package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.local.FavoriteDao
import com.sebastiannarvaez.pokedex.data.local.FavoriteEntity
import com.sebastiannarvaez.pokedex.domain.FavoritePokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Los favoritos, traducidos de fila a dominio.
 *
 * El reloj se inyecta en vez de llamar a `Clock.System` por dentro: asi un test
 * puede fijar la hora y comprobar el orden sin dormir.
 */
internal class FavoritesRepository(
    private val dao: FavoriteDao,
    private val dispatchers: AppDispatchers,
    private val ahora: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    fun observeFavorites(): Flow<List<FavoritePokemon>> =
        dao.observeAll().map { filas ->
            filas.map { FavoritePokemon(it.pokemonId, it.name, PokemonType.deApi(it.primaryType)) }
        }

    /**
     * Solo los identificadores, como conjunto.
     *
     * Es lo que la lista necesita para pintar el corazon, y va aparte a
     * proposito: combinarlo dentro del `PagingData` obligaria a reemitir la
     * pagina entera cada vez que se marca uno, con su parpadeo y su salto de
     * scroll. Un `Set` se consulta en tiempo constante.
     */
    fun observeFavoriteIds(): Flow<Set<Int>> = dao.observeIds().map { it.toSet() }

    suspend fun toggle(id: Int, name: String, primaryType: PokemonType? = null): Boolean = withContext(dispatchers.io) {
        if (dao.isFavorite(id)) {
            dao.remove(id)
            false
        } else {
            dao.add(
                FavoriteEntity(
                    pokemonId = id,
                    name = name,
                    addedAt = ahora(),
                    primaryType = primaryType?.name.orEmpty(),
                ),
            )
            true
        }
    }

    /**
     * Quita un favorito y devuelve su fila tal como estaba, con su fecha, para
     * poder ponerla igual si el usuario se arrepiente. null si no estaba.
     */
    suspend fun quitar(id: Int): FavoriteEntity? = withContext(dispatchers.io) {
        dao.find(id)?.also { dao.remove(id) }
    }

    /** Vuelve a poner una fila quitada. Con su fecha de antes, recupera su sitio en la lista. */
    suspend fun restaurar(fila: FavoriteEntity) = withContext(dispatchers.io) { dao.add(fila) }

    suspend fun clear() = withContext(dispatchers.io) { dao.clear() }
}
