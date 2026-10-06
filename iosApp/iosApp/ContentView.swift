import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

struct ContentView: View {

    /// @StateObject y no @ObservedObject: con @ObservedObject SwiftUI crearía
    /// un dueño nuevo cada vez que se vuelve a construir la vista, el ViewModel se recrearía y el
    /// estado se perdería sin que nada avisara.
    @StateObject private var owner = IosViewModelStoreOwner()

    @State private var estado = HomeUiState(greeting: "", heartbeat: 0)

    var body: some View {
        let viewModel = HomeIosKt.homeViewModel(owner: owner)

        VStack(spacing: 8) {
            Text(estado.greeting)
                .font(.title2)
            Text("Latido \(estado.heartbeat)")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
        .task {
            // El valor actual primero, para no pintar un fotograma vacío.
            estado = viewModel.uiStateForIos
            do {
                for try await nuevo in asyncSequence(for: viewModel.uiStateForIosFlow) {
                    estado = nuevo
                }
            } catch {
                // Aquí solo llegan los errores del flujo: si la vista desaparece, la tarea
                // se cancela y el bucle termina sin pasar por este catch.
            }
        }
    }
}

#Preview {
    ContentView()
}
