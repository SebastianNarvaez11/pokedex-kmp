package com.sebastiannarvaez.pokedex.android.daily

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sebastiannarvaez.pokedex.feature.daily.EstadoDelPermiso

/**
 * El permiso de notificaciones, con sus tres estados.
 *
 * Android no distingue «sin preguntar» de «denegado» en una sola llamada: hay
 * que combinar si estan activadas con si el sistema aconseja explicar por que
 * se piden. Ese `shouldShowRequestPermissionRationale` es true **solo** despues
 * de una denegacion, asi que sirve justo para eso.
 */
fun estadoDelPermiso(context: Context): EstadoDelPermiso = when {
    // Antes de Android 13 no hay permiso en tiempo de ejecucion: basta con que
    // el usuario no las haya desactivado desde los ajustes.
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            EstadoDelPermiso.CONCEDIDO
        } else {
            EstadoDelPermiso.DENEGADO
        }

    NotificationManagerCompat.from(context).areNotificationsEnabled() -> EstadoDelPermiso.CONCEDIDO

    else -> EstadoDelPermiso.SIN_PREGUNTAR
}

/** Lleva a los ajustes del sistema, que es lo unico que queda tras un «no». */
fun abrirAjustesDeNotificaciones(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Recuerda el estado del permiso y da una funcion para pedirlo.
 *
 * El lanzador tiene que crearse en composicion, no dentro de un `onClick`: es
 * la regla de `rememberLauncherForActivityResult` y saltarsela lanza una
 * excepcion en cuanto se usa.
 */
@Composable
fun recordarPermisoDeNotificaciones(): Pair<EstadoDelPermiso, () -> Unit> {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(estadoDelPermiso(context)) }

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        estado = if (concedido) EstadoDelPermiso.CONCEDIDO else EstadoDelPermiso.DENEGADO
    }

    val pedir: () -> Unit = {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                estado == EstadoDelPermiso.SIN_PREGUNTAR ->
                lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)

            // Ya dijo que no: el sistema no vuelve a preguntar por mucho que se
            // lo pidamos. Lo unico honesto es llevarle a los ajustes.
            else -> abrirAjustesDeNotificaciones(context)
        }
    }

    return estado to pedir
}
