package com.sebastiannarvaez.pokedex.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.pokedex.data.FavoritesRepository
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import com.sebastiannarvaez.pokedex.data.local.FavoriteEntity
import com.sebastiannarvaez.pokedex.domain.FavoritePokemon
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoritos: List<FavoritePokemon> = emptyList(),
    val cargando: Boolean = true,
    /**
     * El favorito que se acaba de quitar, mientras la pantalla ofrece
     * deshacerlo. Es un mensaje que se consume: la pantalla lo enseña y avisa
     * con `deshacer()` o con `mensajeMostrado()`. null: no hay nada que enseñar.
     */
    val quitado: FavoritePokemon? = null,
) {
    val vacia: Boolean get() = !cargando && favoritos.isEmpty()
}

class FavoritesViewModel internal constructor(
    private val repository: FavoritesRepository,
    settings: SettingsRepository,
) : ViewModel() {

    // El mensaje vive en el ViewModel, no en la base: es de esta pantalla.
    private val quitado = MutableStateFlow<FavoritePokemon?>(null)
    // La fila tal como estaba, para devolverla con su fecha. No sale del ViewModel.
    private var filaQuitada: FavoriteEntity? = null

    /**
     * La lista, el ajuste de orden y el mensaje de «Deshacer», combinados.
     *
     * El orden se aplica aqui y no en la consulta SQL por una razon practica:
     * cambiar el ajuste no deberia obligar a la base a reconsultar. Con
     * `combine`, el ultimo valor de cada flujo se reordena en memoria, que para
     * una lista de favoritos es inmediato.
     */
    val uiState: StateFlow<FavoritesUiState> =
        combine(repository.observeFavorites(), settings.settings, quitado) { favoritos, ajustes, q ->
            FavoritesUiState(
                favoritos = if (ajustes.favoritosPorNumero) favoritos.sortedBy { it.id } else favoritos,
                cargando = false,
                quitado = q,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    /** Los identificadores, para que la lista pinte el corazon sin recargarse. */
    val favoriteIds: StateFlow<Set<Int>> = repository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggle(id: Int, name: String, primaryType: PokemonType? = null) {
        viewModelScope.launch { repository.toggle(id, name, primaryType) }
    }

    /** Deslizar una fila: la quita y deja el mensaje con su «Deshacer». */
    fun quitar(favorito: FavoritePokemon) {
        viewModelScope.launch {
            filaQuitada = repository.quitar(favorito.id) ?: return@launch
            quitado.value = favorito
        }
    }

    /** La accion del mensaje: lo devuelve a su sitio y consume el mensaje. */
    fun deshacer() {
        val fila = filaQuitada ?: return
        filaQuitada = null
        quitado.value = null
        viewModelScope.launch { repository.restaurar(fila) }
    }

    /** La pantalla ya lo enseño (y se fue sin deshacer): el mensaje se consume. */
    fun mensajeMostrado() {
        filaQuitada = null
        quitado.value = null
    }

    fun vaciar() {
        viewModelScope.launch { repository.clear() }
    }
}
