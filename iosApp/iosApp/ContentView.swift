import SwiftUI
import Shared

struct ContentView: View {
    // Un solo simbolo del modulo compartido. La plataforma se resuelve dentro,
    // donde no ensucia la API que ve Swift.
    private let saludo = Pokedex().greeting()

    var body: some View {
        Text(saludo)
            .font(.title2)
            .padding()
    }
}

#Preview {
    ContentView()
}
