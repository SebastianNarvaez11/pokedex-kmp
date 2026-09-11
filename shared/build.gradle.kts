plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
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
            // Estatico: Xcode solo tiene que enlazarlo. Un framework dinamico
            // habria que incrustarlo y firmarlo en cada compilacion.
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api y no implementation: el Logger asoma en la cara publica del
            // modulo, asi que Swift tiene que poder verlo desde el framework.
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kermit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
