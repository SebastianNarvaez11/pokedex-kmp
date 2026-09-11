package com.sebastiannarvaez.pokedex.android.ui

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
val PokemonType.etiqueta: String
    get() = when (this) {
        PokemonType.NORMAL -> "Normal"
        PokemonType.FIGHTING -> "Lucha"
        PokemonType.FLYING -> "Volador"
        PokemonType.POISON -> "Veneno"
        PokemonType.GROUND -> "Tierra"
        PokemonType.ROCK -> "Roca"
        PokemonType.BUG -> "Bicho"
        PokemonType.GHOST -> "Fantasma"
        PokemonType.STEEL -> "Acero"
        PokemonType.FIRE -> "Fuego"
        PokemonType.WATER -> "Agua"
        PokemonType.GRASS -> "Planta"
        PokemonType.ELECTRIC -> "Eléctrico"
        PokemonType.PSYCHIC -> "Psíquico"
        PokemonType.ICE -> "Hielo"
        PokemonType.DRAGON -> "Dragón"
        PokemonType.DARK -> "Siniestro"
        PokemonType.FAIRY -> "Hada"
    }
