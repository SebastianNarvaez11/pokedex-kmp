package com.sebastiannarvaez.pokedex

import platform.UIKit.UIDevice

private class IosPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun currentPlatform(): Platform = IosPlatform()
