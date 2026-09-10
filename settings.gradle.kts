rootProject.name = "Pokedex"

// Accesores tipados: en vez de project(":shared") se escribe projects.shared,
// que el IDE autocompleta y el compilador comprueba.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// iosApp no esta aqui: es un proyecto de Xcode, no un modulo de Gradle.
include(":shared")
include(":androidApp")
