package com.sebastiannarvaez.pokedex.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

/**
 * El unico test que simula al usuario que ya tenia la app instalada.
 *
 * Abre una base de datos en la version 1 con una fila dentro, aplica la
 * migracion y comprueba que la fila sigue ahi. Es lo que separa una migracion
 * que compila de una que no borra los favoritos de nadie.
 *
 * Vive en `jvmTest` porque `MigrationTestHelper` necesita rutas de fichero y en
 * el simulador de iOS eso se complica. La migracion es la misma en las tres
 * plataformas: el motor va incrustado.
 */
class MigrationTest {

    private val temporal = createTempDirectory("pokedex-migracion")

    private val helper = MigrationTestHelper(
        // El directorio donde estan los esquemas exportados. Si no se
        // versionaran, este test no podria existir.
        schemaDirectoryPath = Path("schemas"),
        databasePath = temporal.resolve("migracion.db"),
        driver = BundledSQLiteDriver(),
        databaseClass = PokedexDatabase::class,
    )

    @Test
    fun losFavoritosSobrevivenALaVersionDos() = runTest {
        // Version 1: la tabla todavia no tiene la columna del tipo.
        helper.createDatabase(version = 1).use { conexion ->
            conexion.execSQL(
                "INSERT INTO favorite (pokemonId, name, addedAt) VALUES (25, 'pikachu', 1000)",
            )
        }

        // Room aplica la migracion automatica y valida que el esquema resultante
        // coincide exactamente con el que declara la version 2.
        helper.runMigrationsAndValidate(version = 2).use { conexion ->
            conexion.prepare("SELECT pokemonId, name, primaryType FROM favorite").use { fila ->
                check(fila.step()) { "la fila desaparecio en la migracion" }
                assertEquals(25, fila.getInt(0))
                assertEquals("pikachu", fila.getText(1))
                // La columna nueva llega con el valor por defecto: sin
                // `defaultValue`, Room no habria podido generar la migracion.
                assertEquals("", fila.getText(2))
            }
        }
    }

    @Test
    fun losFavoritosSobrevivenALaVersionTres() = runTest {
        // Version 2: favoritos con su tipo, y todavia sin la lista guardada.
        helper.createDatabase(version = 2).use { conexion ->
            conexion.execSQL(
                "INSERT INTO favorite (pokemonId, name, addedAt, primaryType) VALUES (25, 'pikachu', 1000, 'ELECTRIC')",
            )
        }

        helper.runMigrationsAndValidate(version = 3).use { conexion ->
            // El favorito sigue ahi, entero: la migracion no toca su tabla.
            conexion.prepare("SELECT pokemonId, primaryType FROM favorite").use { fila ->
                check(fila.step()) { "la fila desaparecio en la migracion" }
                assertEquals(25, fila.getInt(0))
                assertEquals("ELECTRIC", fila.getText(1))
            }
            // Y las dos tablas nuevas existen, vacias: la copia de la lista se
            // llena desde la red la primera vez que se abre la pantalla.
            conexion.prepare("SELECT COUNT(*) FROM pokemon").use { fila ->
                check(fila.step())
                assertEquals(0, fila.getInt(0))
            }
            conexion.prepare("SELECT COUNT(*) FROM remote_keys").use { fila ->
                check(fila.step())
                assertEquals(0, fila.getInt(0))
            }
        }
    }
}
