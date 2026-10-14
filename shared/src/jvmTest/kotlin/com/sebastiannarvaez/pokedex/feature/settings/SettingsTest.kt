package com.sebastiannarvaez.pokedex.feature.settings

import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import com.sebastiannarvaez.pokedex.data.local.createSettingsStore
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SettingsTest {

    // Un repositorio nuevo sobre un fichero temporal nuevo: cada test parte de cero.
    private fun repo(): SettingsRepository {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        return SettingsRepository(createSettingsStore(ruta))
    }

    @Test
    fun sinNadaGuardadoMandaElSistema() = runTest {
        repo().settings.test {
            val inicial = awaitItem()                 // la primera emisión del flujo
            // El último argumento de cada aserción es el mensaje que sale si falla.
            assertEquals(Tema.SISTEMA, inicial.tema, "sin nada guardado, el tema es el del sistema")
            assertFalse(inicial.favoritosPorNumero, "sin nada guardado, lo más reciente va primero")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elCambioLlegaComoEmision() = runTest {
        val repo = repo()

        repo.settings.test {
            assertEquals(Tema.SISTEMA, awaitItem().tema)   // 1.ª emisión: el valor por defecto
            repo.cambiarTema(Tema.OSCURO)                   // se escribe...
            assertEquals(Tema.OSCURO, awaitItem().tema, "tras escribir llega una emisión nueva")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loGuardadoSeLeeDesdeElMismoAlmacen() = runTest {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        val almacen = createSettingsStore(ruta)          // UN almacén...

        SettingsRepository(almacen).cambiarOrden(porNumero = true)

        // ...y un repositorio distinto sobre ese mismo almacén: ve lo guardado.
        SettingsRepository(almacen).settings.test {
            assertTrue(awaitItem().favoritosPorNumero, "otro repositorio sobre el mismo almacén ve lo guardado")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun elTemaGuardadoSeRecuperaPorSuNombre() = runTest {
        val repo = repo()
        repo.cambiarTema(Tema.CLARO)

        repo.settings.test {
            assertEquals(Tema.CLARO, awaitItem().tema, "el tema guardado se recupera por su nombre")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dosAlmacenesSobreElMismoFicheroSeOyen() = runTest {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        val primero = createSettingsStore(ruta)
        SettingsRepository(primero).cambiarOrden(porNumero = true)

        val segundo = createSettingsStore(ruta)   // un SEGUNDO almacén sobre el mismo fichero

        SettingsRepository(segundo).settings.test {
            // DataStore protesta con una `IllegalStateException` al leer del segundo
            // almacén. Como el `catch` del repositorio es estrecho, el error llega.
            val error = awaitError()
            assertIs<IllegalStateException>(error)
        }
    }
}
