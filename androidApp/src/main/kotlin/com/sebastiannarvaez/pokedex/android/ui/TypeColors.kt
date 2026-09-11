package com.sebastiannarvaez.pokedex.android.ui

import androidx.annotation.StringRes
import com.sebastiannarvaez.pokedex.android.R
import androidx.compose.ui.graphics.Color
import com.sebastiannarvaez.pokedex.domain.PokemonType

/**
 * El color de cada tipo, para Android.
 *
 * Vive aqui y no en el modulo compartido a proposito: **el aspecto no se
 * comparte**. Compartir el color obligaria a elegir un espacio de color comun y
 * a renunciar a que cada plataforma use el suyo. Lo que se comparte es el tipo;
 * como se pinta lo decide cada app.
 */
val PokemonType.color: Color
    get() = when (this) {
        PokemonType.NORMAL -> Color(0xFF9099A1)
        PokemonType.FIGHTING -> Color(0xFFCE4069)
        PokemonType.FLYING -> Color(0xFF8FA8DD)
        PokemonType.POISON -> Color(0xFFAB6AC8)
        PokemonType.GROUND -> Color(0xFFD97845)
        PokemonType.ROCK -> Color(0xFFC7B78B)
        PokemonType.BUG -> Color(0xFF90C12C)
        PokemonType.GHOST -> Color(0xFF5269AC)
        PokemonType.STEEL -> Color(0xFF5A8EA1)
        PokemonType.FIRE -> Color(0xFFFF9D55)
        PokemonType.WATER -> Color(0xFF4D90D5)
        PokemonType.GRASS -> Color(0xFF63BC5A)
        PokemonType.ELECTRIC -> Color(0xFFF4D23C)
        PokemonType.PSYCHIC -> Color(0xFFF97176)
        PokemonType.ICE -> Color(0xFF74CEC0)
        PokemonType.DRAGON -> Color(0xFF0B6DC3)
        PokemonType.DARK -> Color(0xFF5A5465)
        PokemonType.FAIRY -> Color(0xFFEC8FE6)
    }

/** El nombre en castellano, que tampoco tiene por que compartirse. */
/**
 * El nombre del tipo, como identificador de recurso.
 *
 * Devuelve el identificador y no el texto porque `stringResource` es
 * `@Composable`: quien pinta resuelve, esto solo dice cual.
 */
@get:StringRes
val PokemonType.etiquetaRes: Int
    get() = when (this) {
        PokemonType.NORMAL -> R.string.tipo_normal
        PokemonType.FIGHTING -> R.string.tipo_fighting
        PokemonType.FLYING -> R.string.tipo_flying
        PokemonType.POISON -> R.string.tipo_poison
        PokemonType.GROUND -> R.string.tipo_ground
        PokemonType.ROCK -> R.string.tipo_rock
        PokemonType.BUG -> R.string.tipo_bug
        PokemonType.GHOST -> R.string.tipo_ghost
        PokemonType.STEEL -> R.string.tipo_steel
        PokemonType.FIRE -> R.string.tipo_fire
        PokemonType.WATER -> R.string.tipo_water
        PokemonType.GRASS -> R.string.tipo_grass
        PokemonType.ELECTRIC -> R.string.tipo_electric
        PokemonType.PSYCHIC -> R.string.tipo_psychic
        PokemonType.ICE -> R.string.tipo_ice
        PokemonType.DRAGON -> R.string.tipo_dragon
        PokemonType.DARK -> R.string.tipo_dark
        PokemonType.FAIRY -> R.string.tipo_fairy
    }
