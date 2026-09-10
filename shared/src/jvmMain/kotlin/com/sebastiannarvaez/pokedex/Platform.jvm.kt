package com.sebastiannarvaez.pokedex

private class JvmPlatform : Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}

actual fun currentPlatform(): Platform = JvmPlatform()
