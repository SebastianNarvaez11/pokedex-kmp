import SwiftUI
import Shared
import UserNotifications

@main
struct iOSApp: App {

    init() {
        // Una sola vez, al abrirse la app. Swift nunca ve Koin: solo esta
        // llamada, que el modulo compartido expone a proposito.
        // El Llavero se pasa aquí porque es de iOS: el núcleo declara la
        // interfaz y la app la cumple.
        #if DEBUG
        // Las pruebas de interfaz arrancan con sesión y sin red. Ver
        // DoblesDePrueba.swift, que explica por qué hace falta.
        KoinIosKt.startKoinIos(
            config: Pruebas.activas ? ConfigDePruebas() : IosAppConfig(),
            secureStorage: Pruebas.activas ? AlmacenDePruebas() : AlmacenDelLlavero()
        )
        #else
        KoinIosKt.startKoinIos(config: IosAppConfig(), secureStorage: AlmacenDelLlavero())
        #endif
        // El delegado hay que ponerlo antes de que la app termine de arrancar,
        // o iOS descarta el toque que abrió la app.
        UNUserNotificationCenter.current().delegate = DelegadoDeNotificaciones.compartido
    }

    var body: some Scene {
        WindowGroup {
            // Sin sesión no se compone nada de la app: ni pestañas, ni pilas.
            RaizConSesion {
                ContentView()
            }
        }
    }
}
