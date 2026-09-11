package com.sebastiannarvaez.pokedex.feature.list

import com.sebastiannarvaez.pokedex.core.UiError
import com.sebastiannarvaez.pokedex.domain.Pokemon

/**
 * La lista paginada, en un solo objeto que una interfaz pueda pintar.
 *
 * En Android esto no hace falta: `paging-compose` ya trae un adaptador que
 * habla con Compose. En iOS no hay equivalente, asi que hay que traducir el
 * flujo de paginas a algo que SwiftUI entienda: una lista y tres banderas.
 */
data class PagedListState(
    val items: List<Pokemon> = emptyList(),
    /** La primera carga, o un tiron para refrescar. */
    val cargando: Boolean = false,
    /** Se esta pidiendo la pagina siguiente. */
    val cargandoMas: Boolean = false,
    /** El error de la carga inicial. Deja la pantalla en blanco con su aviso. */
    val error: UiError? = null,
    /** El error al pedir la pagina siguiente. La lista sigue ahi. */
    val errorAlAmpliar: UiError? = null,
) {
    val vacia: Boolean get() = items.isEmpty() && !cargando && error == null
}
