package com.sebastiannarvaez.pokedex.feature.settings

import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import com.sebastiannarvaez.pokedex.data.local.createSettingsStore
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SettingsTest {

    private fun repo(): SettingsRepository {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        return SettingsRepository(createSettingsStore(ruta))
    }

    @Test
    fun sinNadaGuardadoMandaElSistema() = runTest {
        repo().settings.test {
            val inicial = awaitItem()
            // El valor por defecto es «lo que diga el sistema», no «claro».
            // Elegir por el usuario sin que lo haya pedido es una decision.
            assertEquals(Tema.SISTEMA, inicial.tema)
            assertFalse(inicial.favoritosPorNumero)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elCambioLlegaComoEmision() = runTest {
        val repo = repo()

        repo.settings.test {
            assertEquals(Tema.SISTEMA, awaitItem().tema)
            repo.cambiarTema(Tema.OSCURO)
            // DataStore no tiene lectura sincrona: el cambio se recibe, no se
            // consulta. La pantalla observa su configuracion.
            assertEquals(Tema.OSCURO, awaitItem().tema)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loGuardadoSeLeeDesdeElMismoAlmacen() = runTest {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        val almacen = createSettingsStore(ruta)

        SettingsRepository(almacen).cambiarOrden(porNumero = true)

        // Repositorio nuevo, **mismo almacen**: es lo normal, porque el almacen
        // lo crea la inyeccion una sola vez y vive lo que vive el proceso.
        SettingsRepository(almacen).settings.test {
            assertTrue(awaitItem().favoritosPorNumero)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elTemaGuardadoSeRecuperaPorSuNombre() = runTest {
        val repo = repo()
        repo.cambiarTema(Tema.CLARO)

        repo.settings.test {
            // Se guarda el nombre del enum, no su posicion: reordenar las
            // constantes no debe cambiar el tema de nadie.
            assertEquals(Tema.CLARO, awaitItem().tema)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dosAlmacenesSobreElMismoFicheroSeOyen() = runTest {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        val primero = createSettingsStore(ruta)
        SettingsRepository(primero).cambiarOrden(porNumero = true)

        val segundo = createSettingsStore(ruta)   // un SEGUNDO almacen sobre el mismo fichero

        // Por eso el almacen se registra como `single` en Koin y no se crea en
        // ninguna pantalla. DataStore protesta con una `IllegalStateException`
        // al leer del segundo almacen; como el `catch` del repositorio es
        // estrecho (solo `IOException`), el error llega y no se convierte en
        // unos valores por defecto silenciosos.
        SettingsRepository(segundo).settings.test {
            val error = awaitError()
            assertIs<IllegalStateException>(error)
        }
    }
}
