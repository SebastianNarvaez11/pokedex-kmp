package com.sebastiannarvaez.pokedex.feature.settings

import app.cash.turbine.test
import com.sebastiannarvaez.pokedex.data.SettingsRepository
import com.sebastiannarvaez.pokedex.data.local.createSettingsStore
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {

    private fun repo(): SettingsRepository {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        return SettingsRepository(createSettingsStore(ruta))
    }

    @Test
    fun sinNadaGuardadoManadaElSistema() = runTest {
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
    fun dosAlmacenesSobreElMismoFicheroNoVenLoMismo() = runTest {
        val ruta = createTempDirectory("prefs").resolve("pokedex.preferences_pb").toString()
        val primero = createSettingsStore(ruta)
        SettingsRepository(primero).cambiarOrden(porNumero = true)

        val segundo = createSettingsStore(ruta)

        // Este test documenta un comportamiento incomodo, no lo aprueba.
        //
        // Crear un segundo `DataStore` sobre el mismo fichero **no lanza
        // excepcion**: devuelve los valores por defecto, como si no hubiera
        // nada guardado. Un fallo silencioso es peor que uno ruidoso, porque se
        // manifiesta como «los ajustes no se guardan» en un sitio y bien en
        // otro.
        //
        // Por eso el almacen se registra como `single` en Koin y no se crea en
        // ninguna pantalla. Si alguien rompe esa regla, este test explica que
        // va a ver.
        SettingsRepository(segundo).settings.test {
            assertFalse(awaitItem().favoritosPorNumero, "si esto pasa a ser true, la libreria cambio")
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
}
