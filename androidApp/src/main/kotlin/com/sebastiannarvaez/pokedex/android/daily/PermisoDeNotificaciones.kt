package com.sebastiannarvaez.pokedex.android.daily

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
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
 * Android solo contesta «¿estan activadas?» (`areNotificationsEnabled`): el
 * sistema guarda aparte si el usuario ya contesto, pero esta pregunta no lo
 * devuelve. Por eso, desde Android 13, «desactivadas» se trata como «sin
 * preguntar», y el «no» se aprende cuando llega la respuesta del dialogo.
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

    // Desde Android 13, «desactivadas» puede ser «nunca preguntado» o «dijo que no»,
    // y Android no deja distinguirlo desde aquí. Se trata como sin preguntar.
    else -> EstadoDelPermiso.SIN_PREGUNTAR
}

/** Abre la pantalla de notificaciones de esta app en los ajustes del sistema. */
fun abrirAjustesDeNotificaciones(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Android 8 o superior: la pantalla de notificaciones de esta app.
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        // Android 7: esa pantalla no existe; se abre la ficha de la app.
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    // La función recibe un Context cualquiera: si no fuera una Activity, sin esta
    // bandera Android se negaría a abrir la pantalla.
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
        // Antes de Android 13 el estado nunca es SIN_PREGUNTAR: no hay diálogo que lanzar.
        if (estado == EstadoDelPermiso.SIN_PREGUNTAR) {
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)    // el diálogo del sistema
        } else {
            // Ya se dijo que no: la app no insiste, lleva a los ajustes.
            abrirAjustesDeNotificaciones(context)
        }
    }

    return estado to pedir
}
