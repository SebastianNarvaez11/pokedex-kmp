package com.sebastiannarvaez.pokedex.android.daily

import android.content.Context
import androidx.work.CoroutineWorker                 // un trabajo que puede usar corrutinas
import androidx.work.ExistingPeriodicWorkPolicy      // qué hacer si ya hay uno con ese nombre
import androidx.work.PeriodicWorkRequestBuilder      // la petición «repite esto cada…»
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sebastiannarvaez.pokedex.core.Log
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** La fecha de hoy en la zona del dispositivo, que es la que ve el usuario. */
fun hoy() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

/**
 * El trabajo que muestra la notificación del día.
 * No pide nada a la red: el Pokémon del día es una función de la fecha, así que
 * este trabajo nunca falla por falta de conexión.
 */
class RecordatorioDiarioWorker(
    context: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val fecha = hoy()
        Log.i("recordatorio diario para $fecha")    // una marca para ver que se ejecutó
        mostrarPokemonDelDia(applicationContext, fecha)
        return Result.success()                     // «hecho»; las otras: failure() y retry()
    }
}

const val NOMBRE_TRABAJO = "recordatorio-diario"   // el nombre único del trabajo

fun programarRecordatorioDiario(context: Context) {
    // «Repite el trabajo cada 1 día».
    val trabajo = PeriodicWorkRequestBuilder<RecordatorioDiarioWorker>(1, TimeUnit.DAYS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        NOMBRE_TRABAJO,                       // con nombre: no puede haber dos iguales
        ExistingPeriodicWorkPolicy.KEEP,      // si ya existe, déjalo como está
        trabajo,
    )
}
