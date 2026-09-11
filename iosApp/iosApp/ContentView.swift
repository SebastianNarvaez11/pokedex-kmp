import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

struct ContentView: View {

    /// @StateObject y no @ObservedObject: con @ObservedObject SwiftUI crearía
    /// un dueño nuevo en cada recomposición, el ViewModel se recrearía y el
    /// estado se perdería sin que nada avisara.
    @StateObject private var owner = IosViewModelStoreOwner()

    private let pokedex = Pokedex()

    @State private var estado = HomeUiState(greeting: "", heartbeat: 0)

    var body: some View {
        let viewModel = HomeIosKt.homeViewModel(owner: owner, pokedex: pokedex)

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
                // Cancelar no es fallar: la vista desapareció.
            }
        }
    }
}

#Preview {
    ContentView()
}
