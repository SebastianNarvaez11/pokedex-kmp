import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// Las dos secciones de la app.
///
/// `TabView` es el contenedor de iOS: la barra la pinta el sistema, se oculta
/// sola al entrar en un detalle y en iPad se convierte en barra lateral sin
/// escribir nada. En Android hay que colocar la barra, decidir cuándo se ve y
/// gestionar la pila a mano.
///
/// Cada pestaña lleva **su propia pila**, que es el comportamiento que el
/// usuario de iOS espera: volver a Favoritos te devuelve donde estabas.
struct ContentView: View {

    @State private var caminoLista: [Int32] = []
    @State private var caminoFavoritos: [Int32] = []

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var ajustes = Settings(tema: Tema.sistema, favoritosPorNumero: false)

    var body: some View {
        TabView {
            PokemonListView(camino: $caminoLista)
                .tabItem { Label("Pokédex", systemImage: "square.grid.2x2") }

            FavoritesView(camino: $caminoFavoritos)
                .tabItem { Label("Favoritos", systemImage: "heart.fill") }

            SettingsView()
                .tabItem { Label("Ajustes", systemImage: "gearshape") }
        }
        // El tema se aplica en la raíz: un `.preferredColorScheme` dentro de la
        // pantalla de ajustes solo pintaría esa pestaña.
        .preferredColorScheme(ajustes.tema.esquema)
        .task {
            let vm = SettingsIosKt.settingsViewModel(owner: owner)
            ajustes = vm.settingsForIos
            do {
                for try await nuevo in asyncSequence(for: vm.settingsForIosFlow) {
                    ajustes = nuevo
                }
            } catch { }
        }
    }
}

#Preview {
    ContentView()
}
