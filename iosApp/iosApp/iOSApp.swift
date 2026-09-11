import SwiftUI
import Shared
import UserNotifications

@main
struct iOSApp: App {

    init() {
        // Una sola vez, al abrirse la app. Swift nunca ve Koin: solo esta
        // llamada, que el modulo compartido expone a proposito.
        KoinIosKt.startKoinIos()
        // El delegado hay que ponerlo antes de que la app termine de arrancar,
        // o iOS descarta el toque que abrió la app.
        UNUserNotificationCenter.current().delegate = DelegadoDeNotificaciones.compartido
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
