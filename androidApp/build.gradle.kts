// `java` esta ocupado en un script de Gradle por la extension del plugin de
// Java, asi que `java.util.Properties` no resuelve: hay que importarlo.
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    // NavKey se serializa para sobrevivir a que el sistema mate el proceso.
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.composeCompiler)
}

// Las claves salen de un fichero que no se versiona. Si no existe, quedan
// vacias y la app compila igual: lo unico que pasa es que no hay cuenta.
val secretos = Properties().apply {
    val fichero = rootProject.file("secrets.properties")
    if (fichero.exists()) fichero.inputStream().use { load(it) }
}

android {
    namespace = "com.sebastiannarvaez.pokedex.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sebastiannarvaez.pokedex"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        // Desactivado por defecto desde AGP 8: sin esta linea, `BuildConfig`
        // no se genera y el error dice «unresolved reference», no «actívalo».
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Robolectric necesita los recursos compilados de la app: sin esta
            // linea, cualquier test que infle una vista falla con «resource not
            // found» y el mensaje no menciona a Robolectric.
            isIncludeAndroidResources = true
        }
    }

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${secretos.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${secretos.getProperty("SUPABASE_KEY", "")}\"")
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.paging.compose)
    // Coil 3 no trae fetcher de red: sin coil-network-ktor3 las imagenes no
    // cargan y no hay ningun error visible.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    // Sin esto, todas las entradas comparten el mismo ViewModel y al abrir un
    // detalle se ve el del anterior.
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // viewmodel-compose trae viewModel(); runtime-compose trae
    // collectAsStateWithLifecycle, que es lo que corta el trabajo del Flow
    // cuando la pantalla deja de verse.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    // Los tests de interfaz corren en la JVM con Robolectric, no en un
    // emulador. El motivo es el precio: el job de Linux cuesta la decima parte
    // que el de macOS y no necesita dispositivo. Lo que no cubre esto son los
    // gestos reales y el renderizado del sistema.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Va en `debugImplementation` y no en `testImplementation` porque es un
    // **manifest**, no una libreria: aporta la Activity vacia que necesita
    // `createComposeRule`. En el sitio equivocado, el test falla al arrancar
    // con «No compatible attribute found».
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
