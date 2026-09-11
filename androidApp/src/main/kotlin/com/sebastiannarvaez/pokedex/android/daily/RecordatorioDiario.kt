package com.sebastiannarvaez.pokedex.android.daily

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sebastiannarvaez.pokedex.core.Log
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * El trabajo que muestra la notificacion del dia.
 *
 * No pide nada a la red: el Pokemon del dia es una funcion de la fecha, asi que
 * este worker **nunca falla por falta de conexion**. Es lo que permite no
 * declarar ninguna restriccion de red y que el sistema lo ejecute cuando le
 * venga bien.
 */
class RecordatorioDiarioWorker(
    context: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(context, parametros) {

    @OptIn(ExperimentalTime::class)
    override suspend fun doWork(): Result {
        val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        Log.i("recordatorio diario para $hoy")
        mostrarPokemonDelDia(applicationContext, hoy)
        return Result.success()
    }
}

/**
 * Programa el recordatorio.
 *
 * **WorkManager no es puntual y no pretende serlo.** El minimo de una tarea
 * periodica es de quince minutos, y el sistema la agrupa con otras para no
 * despertar el dispositivo mas de lo necesario. Para un recordatorio diario
 * sobra; para algo que tenga que sonar a una hora exacta haria falta una alarma,
 * que es otro permiso y otra conversacion con el usuario.
 *
 * `KEEP` y no `REPLACE`: si ya estaba programado, reprogramarlo en cada arranque
 * reiniciaria la cuenta y el recordatorio nunca llegaria a dispararse en un
 * dispositivo que se abre a diario.
 */
fun programarRecordatorioDiario(context: Context) {
    val trabajo = PeriodicWorkRequestBuilder<RecordatorioDiarioWorker>(1, TimeUnit.DAYS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        NOMBRE_TRABAJO,
        ExistingPeriodicWorkPolicy.KEEP,
        trabajo,
    )
}

fun cancelarRecordatorioDiario(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(NOMBRE_TRABAJO)
}

const val NOMBRE_TRABAJO = "recordatorio-diario"
