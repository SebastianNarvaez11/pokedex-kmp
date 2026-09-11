package com.sebastiannarvaez.pokedex.feature.daily

/**
 * El estado del permiso de notificaciones, en lenguaje comun.
 *
 * Los tres estados existen en las dos plataformas aunque se llamen distinto, y
 * la pantalla puede razonar sobre ellos sin saber en cual esta.
 */
enum class EstadoDelPermiso {
    /** Todavia no se ha preguntado. Se puede pedir. */
    SIN_PREGUNTAR,

    /** Concedido. */
    CONCEDIDO,

    /**
     * Denegado. **Ya no se puede volver a preguntar**: hay que mandar al
     * usuario a los ajustes del sistema. Es la diferencia que mas se olvida.
     */
    DENEGADO,
}

/**
 * Programar el recordatorio es cosa de cada plataforma.
 *
 * Es una interfaz y no un `expect`: el nucleo decide **que** recordar y
 * **cuando**, y cada app decide **como**. Ademas, asi se puede falsear en un
 * test sin tocar el sistema de notificaciones.
 */
interface DailyReminderScheduler {
    suspend fun estadoDelPermiso(): EstadoDelPermiso
    suspend fun pedirPermiso(): EstadoDelPermiso
    suspend fun programar(horaLocal: Int)
    suspend fun cancelar()
}
