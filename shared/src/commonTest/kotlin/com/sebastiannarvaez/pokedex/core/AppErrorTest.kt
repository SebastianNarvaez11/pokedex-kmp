package com.sebastiannarvaez.pokedex.core

import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppErrorTest {

    @Test
    fun unFalloDeRedEsSinConexion() {
        assertEquals(AppError.SinConexion, IOException("socket cerrado").aAppError())
    }

    @Test
    fun laCancelacionNoSeTraduce() {
        // Tragarse una cancelacion rompe la concurrencia estructurada: la
        // corrutina sigue viva despues de que su pantalla desaparezca.
        assertFailsWith<CancellationException> {
            CancellationException("la vista se fue").aAppError()
        }
    }

    @Test
    fun soloSeOfreceReintentarCuandoTieneSentido() {
        assertTrue(AppError.SinConexion.esReintentable)
        assertTrue(AppError.Lento.esReintentable)
        assertTrue(AppError.Servidor(503).esReintentable)
        // Un 404 va a seguir siendo 404 por muchas veces que se pulse.
        assertFalse(AppError.Servidor(404).esReintentable)
        assertFalse(AppError.Inesperado("vaya").esReintentable)
    }

    @Test
    fun lasDosAppsPintanElMismoTexto() {
        // El mapeo es compartido a proposito: si cada interfaz escribiera el
        // suyo, una diria «sin conexion» y la otra «no hay internet».
        val error = AppError.SinConexion.aUiError()

        assertEquals("Sin conexión", error.titulo)
        assertTrue(error.sePuedeReintentar)
    }

    @Test
    fun unErrorDelServidorLlevaSuCodigoAlTexto() {
        val error = AppError.Servidor(503).aUiError()

        assertTrue(error.detalle.contains("503"), "detalle inesperado: ${error.detalle}")
        assertTrue(error.sePuedeReintentar)
    }
}
