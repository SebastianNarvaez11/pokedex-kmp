package com.sebastiannarvaez.pokedex.android.navigation

import androidx.navigation3.runtime.NavKey
import com.sebastiannarvaez.pokedex.navigation.Destination
import kotlinx.serialization.Serializable

/**
 * Las claves de Navigation 3, que son de Android.
 *
 * El mapa de pantallas vive en el modulo compartido; esto es solo su forma
 * local. Se traduce en la frontera y no antes: meter `NavKey` en `commonMain`
 * ataria el nucleo a una libreria de interfaz que iOS no usa.
 *
 * Son `@Serializable` porque Navigation 3 guarda la pila para sobrevivir a que
 * el sistema mate el proceso: sin eso, volver a la app tras un rato en segundo
 * plano devuelve a la pantalla inicial.
 */
@Serializable
data object ListaKey : NavKey

@Serializable
data object FavoritosKey : NavKey

@Serializable
data class DetalleKey(val pokemonId: Int) : NavKey

/** Las pestañas de primer nivel, que nunca se apilan entre si. */
val PESTANAS: List<NavKey> = listOf(ListaKey, FavoritosKey)

fun Destination.aNavKey(): NavKey = when (this) {
    Destination.Lista -> ListaKey
    is Destination.Detalle -> DetalleKey(pokemonId)
}
