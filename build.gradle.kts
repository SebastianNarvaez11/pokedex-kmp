// La raiz no construye nada: solo declara que plugins existen y en que version,
// para que cada modulo los aplique sin repetir el numero.
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.nativeCoroutines) apply false
}
