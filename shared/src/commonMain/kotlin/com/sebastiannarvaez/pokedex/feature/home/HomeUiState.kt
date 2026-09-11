package com.sebastiannarvaez.pokedex.feature.home

/**
 * Todo lo que la pantalla necesita para pintarse, en un solo objeto.
 *
 * Es una `data class` y no una `sealed interface` a proposito. Una jerarquia
 * sellada se lee muy bien en Kotlin, pero cruza mal a Swift: pierde la
 * exhaustividad, y el `switch` de SwiftUI acaba con un `default` que nadie
 * revisa. Los estados sellados se quedan puertas adentro.
 */
data class HomeUiState(
    val greeting: String = "",
    val heartbeat: Int = 0,
)
