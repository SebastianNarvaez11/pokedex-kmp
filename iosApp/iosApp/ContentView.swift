import SwiftUI
import Shared

struct ContentView: View {
    // El saludo viene del modulo compartido: es la prueba de que iOS esta
    // consumiendo el mismo Kotlin que Android.
    private let saludo = Greeting().greet()

    var body: some View {
        Text(saludo)
            .font(.title2)
            .padding()
    }
}

#Preview {
    ContentView()
}
