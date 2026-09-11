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

    /// La pestaña visible. Hace falta como estado porque un enlace entrante
    /// tiene que poder cambiarla: si llega una ficha estando en Favoritos, hay
    /// que saltar a la Pokédex antes de apilar nada.
    @State private var pestana = Pestana.lista

    var body: some View {
        TabView(selection: $pestana) {
            PokemonListView(camino: $caminoLista)
                .tabItem { Label("Pokédex", systemImage: "square.grid.2x2") }
                .tag(Pestana.lista)

            FavoritesView(camino: $caminoFavoritos)
                .tabItem { Label("Favoritos", systemImage: "heart.fill") }
                .tag(Pestana.favoritos)

            SettingsView()
                .tabItem { Label("Ajustes", systemImage: "gearshape") }
                .tag(Pestana.ajustes)
        }
        // El tema se aplica en la raíz: un `.preferredColorScheme` dentro de la
        // pantalla de ajustes solo pintaría esa pestaña.
        .preferredColorScheme(ajustes.tema.esquema)
        // `.onOpenURL` cubre los dos casos, app cerrada y app abierta, sin que
        // haya que distinguirlos. En Android son dos sitios: onCreate y
        // onNewIntent, y olvidar el segundo es el fallo clásico.
        .onOpenURL { url in
            // `DestinationCompanion.shared` y no `Destination.companion`: una
            // interfaz sellada llega a Swift como **protocolo**, y un protocolo
            // no tiene companion. El objeto acompanante se expone aparte, con
            // el nombre de la interfaz pegado.
            guard let destino = DestinationCompanion.shared.parse(url: url.absoluteString) else { return }
            pestana = .lista
            if let detalle = destino as? DestinationDetalle {
                caminoLista = [detalle.pokemonId]
            } else {
                caminoLista = []
            }
        }
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

private enum Pestana: Hashable {
    case lista, favoritos, ajustes
}
