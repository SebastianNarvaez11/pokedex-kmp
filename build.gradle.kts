// La raiz no construye nada: solo declara que plugins existen y en que version,
// para que cada modulo los aplique sin repetir el numero.
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.nativeCoroutines) apply false
}

/**
 * Que ocupa lo que hemos construido.
 *
 * No mide nada por su cuenta: lee lo que ya hay en `build/`. La idea es que el
 * tamano sea un numero que se mira, no una sorpresa el dia de publicar. Antes
 * hay que construir lo que se quiera medir; lo que falte sale como «(sin
 * construir)».
 */
tasks.register("tamanos") {
    group = "verification"
    description = "Imprime el tamano de los artefactos ya construidos."

    val piezas = mapOf(
        "Framework iOS · debug (simulador)" to
            "shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework",
        "Framework iOS · release (iosArm64)" to
            "shared/build/bin/iosArm64/releaseFramework/Shared.framework",
        "APK debug" to "androidApp/build/outputs/apk/debug/androidApp-debug.apk",
        "APK release sin firmar" to
            "androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk",
    ).mapValues { (_, ruta) -> rootProject.file(ruta) }

    doLast {
        piezas.forEach { (nombre, fichero) ->
            val bytes = when {
                !fichero.exists() -> -1L
                fichero.isDirectory -> fichero.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                else -> fichero.length()
            }
            val texto = if (bytes < 0) "(sin construir)" else "%.1f MB".format(bytes / 1024.0 / 1024.0)
            println("%-40s %s".format(nombre, texto))
        }
    }
}
