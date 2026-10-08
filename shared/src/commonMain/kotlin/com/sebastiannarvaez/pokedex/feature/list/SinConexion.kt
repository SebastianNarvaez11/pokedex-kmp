package com.sebastiannarvaez.pokedex.feature.list

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

/**
 * ¿La lista esta ensenando lo guardado porque la red ha fallado?
 *
 * Con un mediador hay dos estados de carga: el de la **fuente** (la base, que
 * casi nunca falla) y el del **mediador** (la red). Si falla el mediador y hay
 * filas, la lista se sigue viendo y solo hace falta avisar. Si no hay filas,
 * no es un aviso: es la pantalla de error de siempre, con su reintento.
 *
 * La regla vive aqui, en comun, para que las dos apps avisen exactamente en
 * los mismos casos.
 */
fun CombinedLoadStates.mostrandoLoGuardado(hayFilas: Boolean): Boolean =
    hayFilas && (mediator?.refresh is LoadState.Error || mediator?.append is LoadState.Error)
