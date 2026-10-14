package com.sebastiannarvaez.pokedex.android.ui

import android.graphics.Color.TRANSPARENT
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sebastiannarvaez.pokedex.feature.settings.Tema

private val Rojo = Color(0xFFD2413C)

private val ClaroPropio = lightColorScheme(
    primary = Rojo,
    secondary = Color(0xFF4D90D5),
    tertiary = Color(0xFFF4D23C),
)

private val OscuroPropio = darkColorScheme(
    primary = Color(0xFFFFB4AA),
    secondary = Color(0xFF9FCAFF),
    tertiary = Color(0xFFE9C84A),
)

/**
 * Color dinamico donde lo haya, y una paleta propia donde no.
 *
 * El color dinamico es de Android: toma los tonos del fondo de pantalla del
 * usuario. No existe en iOS, y ese es justo el punto de tener dos interfaces
 * nativas en vez de una compartida.
 */
@Composable
fun PokedexTheme(
    tema: Tema = Tema.SISTEMA,
    contenido: @Composable () -> Unit,
) {
    val oscuro = when (tema) {
        Tema.SISTEMA -> isSystemInDarkTheme()
        Tema.CLARO -> false
        Tema.OSCURO -> true
    }

    val contexto = LocalContext.current
    val esquema = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        oscuro -> OscuroPropio
        else -> ClaroPropio
    }

    // Los iconos de las barras del sistema (la hora, la bateria) siguen al tema
    // de la app y no al del movil. Sin esto, con «Oscuro» y el movil en claro,
    // salen oscuros sobre fondo oscuro. En una vista previa no hay actividad.
    val actividad = LocalActivity.current as? ComponentActivity
    DisposableEffect(actividad, oscuro) {
        actividad?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { oscuro },
            navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { oscuro },
        )
        onDispose {}
    }

    MaterialTheme(colorScheme = esquema, content = contenido)
}
