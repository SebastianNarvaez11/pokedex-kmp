package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import com.sebastiannarvaez.pokedex.domain.Session
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureStorageTokenStoreTest {

    private val sesion = Session(
        accessToken = "acceso",
        refreshToken = "refresco",
        expiresAtEpochSeconds = 4_600,
        userId = "u-1",
        email = "ash@pueblo-paleta.test",
    )

    private fun TestScope.store(almacen: SecureStorage = InMemorySecureStorage()) =
        SecureStorageTokenStore(almacen, TestDispatchers(StandardTestDispatcher(testScheduler)))

    @Test
    fun `lo que se guarda se recupera igual`() = runTest {
        val s = store()

        s.guardar(sesion)

        assertEquals(sesion, s.leer())
    }

    @Test
    fun `sin nada guardado no hay sesion`() = runTest {
        assertNull(store().leer())
    }

    @Test
    fun `borrar deja el almacen vacio`() = runTest {
        val almacen = InMemorySecureStorage()
        val s = store(almacen)
        s.guardar(sesion)

        s.borrar()

        assertNull(s.leer())
        assertNull(almacen.leer("sesion"))
    }

    /**
     * Lo guardado tiene que sobrevivir a una actualizacion de la app. Si el
     * formato cambia y ya no encaja, lo correcto es tratarlo como «no hay
     * sesion» y pedir que entre otra vez, no reventar al arrancar.
     */
    @Test
    fun `un contenido ilegible se trata como si no hubiera sesion`() = runTest {
        val almacen = InMemorySecureStorage()
        almacen.guardar("sesion", "{esto no es json}")

        assertNull(store(almacen).leer())
    }

    /**
     * Los nombres de los campos en disco estan fijados a proposito y no siguen
     * a los de `Session`. Renombrar una propiedad del dominio no debe cerrarle
     * la sesion a quien ya tenia la app.
     */
    @Test
    fun `el formato en disco no arrastra los nombres del dominio`() = runTest {
        val almacen = InMemorySecureStorage()
        store(almacen).guardar(sesion)

        val guardado = almacen.leer("sesion")!!

        assertTrue(guardado.contains("\"acceso\""), guardado)
        assertTrue(guardado.contains("\"caducaEn\""), guardado)
        assertTrue(!guardado.contains("accessToken"), guardado)
    }
}
