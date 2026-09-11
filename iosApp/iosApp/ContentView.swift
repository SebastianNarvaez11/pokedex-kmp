import SwiftUI
import Shared

struct ContentView: View {
    // El saludo viene del modulo compartido: es la prueba de que iOS esta
    // consumiendo el mismo Kotlin que Android.
    //
    // `Platform_iosKt` chirria, y con razon: el nombre sale del fichero donde
    // vive el `actual`, que es Platform.ios.kt. Se arregla en la leccion de
    // funciones que cruzan a Swift; de momento se deja a la vista.
    private let saludo = Greeting(platform: Platform_iosKt.currentPlatform()).greet()

    var body: some View {
        Text(saludo)
            .font(.title2)
            .padding()
    }
}

#Preview {
    ContentView()
}
