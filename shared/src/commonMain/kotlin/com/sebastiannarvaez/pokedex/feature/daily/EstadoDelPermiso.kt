package com.sebastiannarvaez.pokedex.feature.daily

/**
 * El estado del permiso de notificaciones, en lenguaje común.
 * Los tres existen en las dos plataformas aunque se llamen distinto.
 */
enum class EstadoDelPermiso {
    /** Todavía no se ha preguntado. Se puede pedir. */
    SIN_PREGUNTAR,

    /** Concedido. */
    CONCEDIDO,

    /**
     * Denegado. Tras decir que no (en iOS, una vez; en Android, dos), el sistema
     * ya no enseña el diálogo: hay que mandar al usuario a los ajustes del sistema.
     */
    DENEGADO,
}
