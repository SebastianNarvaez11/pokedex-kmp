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

// La firma de release. Si el fichero no esta, la app se compila **sin firmar**
// en vez de fallar: quien clone el repositorio puede construirla para probar
// sin tener que inventarse un almacen de claves.
val firma = Properties().apply {
    val fichero = rootProject.file("keystore.properties")
    if (fichero.exists()) fichero.inputStream().use { load(it) }
}
val hayFirma = firma.getProperty("RELEASE_STORE_PASSWORD").orEmpty().isNotBlank()

android {
    namespace = "com.sebastiannarvaez.pokedex.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sebastiannarvaez.pokedex"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        // Del catalogo, que es el unico sitio donde se escribe un numero.
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
        // Cada constante acaba en BuildConfig.SUPABASE_URL / SUPABASE_KEY. El
        // segundo argumento de getProperty es el valor si falta la clave; las
        // comillas escapadas hacen que la constante sea un texto en Java.
        buildConfigField("String", "SUPABASE_URL", "\"${secretos.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${secretos.getProperty("SUPABASE_KEY", "")}\"")
    }

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = rootProject.file(firma.getProperty("RELEASE_STORE_FILE"))
                storePassword = firma.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = firma.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = firma.getProperty("RELEASE_KEY_PASSWORD")
                // v1 desactivado: desde minSdk 24 no hace falta, y quitarlo
                // acelera la firma y evita el zip que se puede alterar.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (hayFirma) signingConfig = signingConfigs.getByName("release")
            // La DSL nueva de AGP 9.3: una linea enciende **las dos** cosas,
            // optimizacion de codigo y de recursos, y ya trae las reglas de la
            // plataforma. Antes habia que activarlas por separado y nombrar el
            // fichero `proguard-android-optimize.txt` a mano.
            optimization {
                enable = true
            }
        }
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
    // La pantalla de arranque oficial. Desde Android 12 el sistema **siempre**
    // pinta una; esta libreria deja decidir que se ve en ella y la hace
    // funcionar igual en versiones anteriores.
    implementation(libs.androidx.core.splashscreen)
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
