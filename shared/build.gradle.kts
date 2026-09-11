plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    // Plugin de compilador, no de KSP: convierte los Flow anotados en algo que
    // Swift pueda recorrer con `for await`.
    alias(libs.plugins.nativeCoroutines)
}

kotlin {
    // La JVM no es una plataforma de producto: esta para que los tests corran
    // en segundos, sin emulador ni simulador de por medio.
    jvm()

    // `android {}` dentro de `kotlin {}` es el bloque del plugin KMP de AGP.
    // Sustituye a `androidLibrary {}`, obsoleto desde AGP 9.1.
    android {
        namespace = "com.sebastiannarvaez.pokedex.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        // Habilita el source set de tests que corren en la maquina, no en un
        // dispositivo: es el equivalente de `test` en un modulo Android normal.
        withHostTest {}
    }

    // Sin iosX64: el simulador de los Mac con Intel se queda fuera. Es una
    // decision, no un descuido, y se explica en la leccion de persistencia.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            // Sin este export, `ViewModel` llega a Swift como el tipo opaco
            // `Lifecycle_viewmodelViewModel` y no se puede heredar de el.
            export(libs.androidx.lifecycle.viewmodel)
            // Estatico: Xcode solo tiene que enlazarlo. Un framework dinamico
            // habria que incrustarlo y firmarlo en cada compilacion.
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api y no implementation: los ViewModel son parte de la cara
            // publica del modulo, porque quien los crea es cada interfaz.
            api(libs.androidx.lifecycle.viewmodel)
            // api: el grafo de Koin es parte de la cara publica del modulo,
            // porque quien arranca la app es cada interfaz nativa, no esto.
            // api: PagingData asoma en la cara publica del ViewModel.
            // api: la base de datos asoma en el constructor del repositorio,
            // asi que sus supertipos tienen que viajar con el modulo.
            api(libs.androidx.room.runtime)
            // Bundled y no el SQLite del sistema: la misma version del motor en
            // Android, iOS y JVM, y en iOS ahorra enlazar -lsqlite3 a mano.
            implementation(libs.androidx.sqlite.bundled)
            api(libs.androidx.paging.common)
            api(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            // api y no implementation: el Logger asoma en la cara publica del
            // modulo, asi que Swift tiene que poder verlo desde el framework.
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            // Cada plataforma usa su motor: el cliente de Ktor es comun, quien
            // hace la peticion de verdad no lo es.
            implementation(libs.ktor.client.okhttp)
        }
        // Solo iOS: la anotacion que hace recorrible un Flow desde Swift no
        // pinta nada en Android ni en la JVM.
        iosMain.dependencies {
            implementation(libs.kmp.nativecoroutines.annotations)
            implementation(libs.kmp.nativecoroutines.core)
            implementation(libs.ktor.client.darwin)
        }
        // koin-test verifica el grafo por reflexion, que Kotlin/Native no tiene.
        jvmTest.dependencies {
            implementation(libs.koin.test)
            implementation(libs.androidx.room.testing)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
            implementation(libs.androidx.paging.testing)
        }
    }
}

room3 {
    // Los esquemas generados se versionan con el codigo: son la unica forma de
    // escribir una migracion automatica, y sin ellos Room no puede compararla.
    schemaDirectory("$projectDir/schemas")
}

// Room genera el codigo con KSP, y KSP no tiene noticia de los source sets
// comunes: hay que pedirle el procesador target por target. Si falta uno, ese
// target compila sin DAOs y falla en ejecucion, no al compilar.
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}
