package com.sebastiannarvaez.pokedex.android.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Las claves de Navigation 3, que son de Android.
 *
 * El mapa de pantallas vive en el modulo compartido; esto es solo su forma
 * local. Se traduce en la frontera y no antes: meter `NavKey` en `commonMain`
 * haria depender al nucleo de una libreria de AndroidX que iOS no usa.
 *
 * Son `@Serializable` porque Navigation 3 guarda la pila para sobrevivir a que
 * el sistema mate el proceso: sin eso, volver a la app tras un rato en segundo
 * plano devuelve a la pantalla inicial.
 */
@Serializable
data object ListaKey : NavKey

@Serializable
data class DetalleKey(val pokemonId: Int) : NavKey
