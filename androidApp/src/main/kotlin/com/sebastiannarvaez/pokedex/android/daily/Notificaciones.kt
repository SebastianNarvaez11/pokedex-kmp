package com.sebastiannarvaez.pokedex.android.daily

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sebastiannarvaez.pokedex.android.R
import com.sebastiannarvaez.pokedex.feature.daily.PokemonOfTheDay
import com.sebastiannarvaez.pokedex.navigation.Destination
import com.sebastiannarvaez.pokedex.navigation.toDeepLink
import kotlinx.datetime.LocalDate

const val CANAL_DIARIO = "pokemon_del_dia"

/**
 * Crea el canal de notificaciones.
 *
 * Desde Android 8 **toda** notificacion necesita canal: sin el, no se muestra y
 * no hay error visible. Crear un canal que ya existe no hace nada, asi que se
 * puede llamar en cada arranque sin comprobar.
 *
 * El nombre y la descripcion los ve el usuario en los ajustes del sistema: son
 * texto de producto, no detalle tecnico.
 */
fun crearCanalDiario(context: Context) {
    val canal = NotificationChannel(
        CANAL_DIARIO,
        // Fuera de Compose no hay `stringResource`: aqui se usa el `Context`,
        // que es lo que `stringResource` acaba usando por dentro. El nombre del
        // canal lo ve el usuario en los ajustes del sistema, asi que se traduce
        // igual que el resto.
        context.getString(R.string.dia_titulo),
        // DEFAULT y no HIGH: esto no es urgente. Una notificacion diaria que
        // suena e interrumpe es una notificacion que el usuario desactiva.
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.dia_canal_descripcion)
    }

    NotificationManagerCompat.from(context).createNotificationChannel(canal)
}

/**
 * Muestra la notificacion del dia.
 *
 * El toque abre la ficha con el mismo enlace profundo que funciona desde fuera
 * de la app: una sola forma de llegar a una pantalla, no dos.
 */
fun mostrarPokemonDelDia(context: Context, fecha: LocalDate) {
    val id = PokemonOfTheDay.idParaFecha(fecha)
    val enlace = Destination.Detalle(id).toDeepLink()

    val intent = PendingIntent.getActivity(
        context,
        id,
        Intent(Intent.ACTION_VIEW, Uri.parse(enlace)).setPackage(context.packageName),
        // IMMUTABLE es obligatorio desde Android 12 si no se va a rellenar
        // despues. Sin una de las dos banderas, la app revienta al crearlo.
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val notificacion = NotificationCompat.Builder(context, CANAL_DIARIO)
        .setSmallIcon(R.drawable.ic_notificacion)
        .setContentTitle(context.getString(R.string.dia_canal))
        .setContentText(context.getString(R.string.dia_notificacion_texto, id))
        .setContentIntent(intent)
        .setAutoCancel(true)
        .build()

    // El permiso puede faltar: notify() no lanza, simplemente no muestra nada.
    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        NotificationManagerCompat.from(context).notify(ID_NOTIFICACION, notificacion)
    }
}

private const val ID_NOTIFICACION = 1
