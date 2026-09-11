import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

struct ContentView: View {
    // Un solo simbolo del modulo compartido. La plataforma se resuelve dentro,
    // donde no ensucia la API que ve Swift.
    private let pokedex = Pokedex()

    @State private var latido = 0

    var body: some View {
        VStack(spacing: 12) {
            Text(pokedex.greeting())
                .font(.title2)
            Text("Latido \(latido)")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
        // .task se cancela solo cuando la vista desaparece, y con ella se
        // cancela la corrutina de Kotlin: el Flow es frio y deja de emitir.
        .task {
            do {
                for try await valor in asyncSequence(for: pokedex.heartbeatForIos()) {
                    latido = valor.intValue
                }
            } catch {
                // Un Flow cancelado llega aqui como error. No es un fallo.
            }
        }
    }
}

#Preview {
    ContentView()
}
