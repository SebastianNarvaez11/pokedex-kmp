package com.sebastiannarvaez.pokedex.android.daily

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult   // el diálogo, desde Compose
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.sebastiannarvaez.pokedex.feature.daily.EstadoDelPermiso

fun estadoDelPermiso(context: Context): EstadoDelPermiso = when {
    // Antes de Android 13 no hay permiso en tiempo de ejecución: basta con que
    // el usuario no las haya desactivado en los ajustes.
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->    // TIRAMISU = Android 13
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            EstadoDelPermiso.CONCEDIDO
        } else {
            EstadoDelPermiso.DENEGADO
        }

    NotificationManagerCompat.from(context).areNotificationsEnabled() -> EstadoDelPermiso.CONCEDIDO

    // Desde Android 13, «desactivadas» puede ser «nunca preguntado» o «dijo que no»,
    // y Android no deja distinguirlo desde aquí. Se trata como sin preguntar.
    else -> EstadoDelPermiso.SIN_PREGUNTAR
}

/** Abre los ajustes del sistema para que el usuario active las notificaciones de esta app. */
fun abrirAjustesDeNotificaciones(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Android 8 o superior: la pantalla de notificaciones de esta app.
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        // Android 7: esa pantalla no existe; se abre la ficha de la app.
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    // NEW_TASK se añade porque la función recibe un Context cualquiera: si no fuera una
    // Activity, sin esta bandera Android se negaría a abrir la pantalla.
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Devuelve el estado actual y una función para pedir el permiso. */
@Composable
fun recordarPermisoDeNotificaciones(): Pair<EstadoDelPermiso, () -> Unit> {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(estadoDelPermiso(context)) }

    // El lanzador es un composable: se crea al dibujar la pantalla, nunca dentro de
    // un onClick (el compilador lo rechaza). La lambda recibe la respuesta del diálogo.
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        estado = if (concedido) EstadoDelPermiso.CONCEDIDO else EstadoDelPermiso.DENEGADO
    }

    val pedir: () -> Unit = {
        if (estado == EstadoDelPermiso.SIN_PREGUNTAR) {
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)    // el diálogo del sistema
        } else {
            // Ya se dijo que no: la app no insiste, lleva a los ajustes.
            abrirAjustesDeNotificaciones(context)
        }
    }

    return estado to pedir
}
