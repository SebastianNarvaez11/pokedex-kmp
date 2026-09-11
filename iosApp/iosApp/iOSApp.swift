import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        // Una sola vez, al abrirse la app. Swift nunca ve Koin: solo esta
        // llamada, que el modulo compartido expone a proposito.
        KoinIosKt.startKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
