// `Properties` es la clase de Java que lee ficheros de ajustes `clave=valor`.
// `java` esta ocupado en un script de Gradle por la extension del plugin de
// Java, asi que `java.util.Properties` sin import no resuelve.
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    // NavKey se serializa para sobrevivir a que el sistema mate el proceso.
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.composeCompiler)
}

// Lee `local.properties` (en la carpeta raiz del proyecto). Si no existe,
// `secretos` queda vacio y la app compila igual: lo unico que pasa es que no
// hay cuenta.
val secretos = Properties().apply {
    val fichero = rootProject.file("local.properties")
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
        // Cada constante acaba en BuildConfig.SUPABASE_URL / SUPABASE_KEY. El
        // segundo argumento de getProperty es el valor si falta la clave; las
        // comillas escapadas hacen que la constante sea un texto en Java.
        buildConfigField("String", "SUPABASE_URL", "\"${secretos.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${secretos.getProperty("SUPABASE_KEY", "")}\"")
    }

    buildFeatures {
        compose = true
        // Desactivado por defecto desde AGP 8: sin esta linea, `BuildConfig`
        // no se genera y el error dice «unresolved reference», no «actívalo».
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Robolectric necesita los recursos y el manifiesto ya compilados
            // de la app: sin esta línea no encuentra la Activity de prueba.
            isIncludeAndroidResources = true
        }
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
    // debugImplementation: solo entra en la app de depuracion; la de publicar no la lleva.
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    // testImplementation: solo existe al compilar y ejecutar los tests; la app
    // que instalas no las lleva (como en Marcador).
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(kotlin("test"))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // debugImplementation: va dentro de la app de depuración, no solo de los
    // tests, porque es un manifiesto con una Activity vacía. Es el mismo tipo
    // de dependencia que LeakCanary en Marcador.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
