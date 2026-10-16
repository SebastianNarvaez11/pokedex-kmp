import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// Las tres secciones de la app, como valores que `TabView` puede seleccionar.
// `Hashable` (visto en Marcador): SwiftUI puede comparar estos valores.
private enum Pestana: Hashable {
    case lista, favoritos, ajustes
}

struct ContentView: View {

    // Un camino de fichas abiertas por pestaña: cada pestaña lleva su pila.
    @State private var caminoLista: [Int32] = []
    @State private var caminoFavoritos: [Int32] = []

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var ajustes = Settings(tema: Tema.sistema, favoritosPorNumero: false)
    @State private var insignia: String? = nil   // nil: sin insignia en Favoritos
    @StateObject private var notificaciones = DelegadoDeNotificaciones.compartido
    @Environment(\.scenePhase) private var scenePhase

    // La pestaña visible. Tiene que ser estado porque un enlace
    // entrante la cambia: si llega una ficha estando en Favoritos, hay que
    // saltar a la Pokédex antes de apilar nada.
    @State private var pestana = Pestana.lista

    var body: some View {
        TabView(selection: $pestana) {   // `selection` ata el TabView al estado
            PokemonListView(camino: $caminoLista)
                .tabItem { Label("Pokédex", systemImage: "square.grid.2x2") }
                .tag(Pestana.lista)       // el valor que representa esta pestaña

            FavoritesView(camino: $caminoFavoritos)
                .tabItem { Label("Favoritos", systemImage: "heart.fill") }
                .tag(Pestana.favoritos)
                .badge(insignia)   // con nil SwiftUI no pinta nada

            SettingsView()
                .tabItem { Label("Ajustes", systemImage: "gearshape") }
                .tag(Pestana.ajustes)
        }
        // El tema se aplica en la raíz: dentro de la pantalla de ajustes solo
        // pintaría esa pestaña.
        .preferredColorScheme(ajustes.tema.esquema)
        // Reponer los avisos cada vez que la app pasa a primer plano: siete días
        // se acaban, y sin esto la app dejaría de avisar a la semana.
        // `initial: true` también lo ejecuta al arrancar, no solo al volver.
        .onChange(of: scenePhase, initial: true) { _, fase in
            guard fase == .active else { return }
            Task {
                // Solo si el usuario ya dijo que sí: reprogramar sin permiso no hace nada.
                if await RecordatorioDiario.estadoDelPermiso() == .concedido {
                    await RecordatorioDiario.programar()
                }
            }
        }
        // `.onOpenURL` cubre los dos casos, app cerrada y app abierta,
        // sin distinguirlos. En Android son dos sitios (`onCreate` y
        // `onNewIntent`), y olvidar el segundo hace que el enlace falle si la app ya estaba abierta.
        // El toque en una notificación acaba en el mismo sitio que un enlace
        // externo: no hay dos caminos a la misma pantalla.
        .onChange(of: notificaciones.enlacePendiente) { _, url in
            guard let url else { return }
            abrir(url)
            notificaciones.enlacePendiente = nil
        }
        .onOpenURL { url in
            // `DestinationCompanion.shared` y no `Destination.companion`: una
            // interfaz sellada llega a Swift como **protocolo**, y un protocolo
            // no tiene companion. El objeto acompanante se expone aparte, con
            // el nombre de la interfaz pegado.
            abrir(url)
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
        // La insignia de Favoritos: otro lector del mismo repositorio de favoritos.
        .task {
            let favoritos = FavoritesIosKt.favoritesViewModel(owner: owner)
            insignia = favoritos.insigniaForIos
            do {
                for try await nuevo in asyncSequence(for: favoritos.insigniaForIosFlow) {
                    insignia = nuevo
                }
            } catch { }
        }
    }
}

#Preview {
    ContentView()
}

private extension ContentView {
    func abrir(_ url: URL) {
        // `DestinationCompanion.shared` y no `Destination.companion`: una
        // interfaz sellada llega a Swift como **protocolo**, y un protocolo no
        // tiene companion. El objeto acompanante se expone aparte.
        guard let destino = DestinationCompanion.shared.parse(url: url.absoluteString) else { return }
        pestana = .lista
        if let detalle = destino as? DestinationDetalle {
            caminoLista = [detalle.pokemonId]
        } else {
            caminoLista = []
        }
    }
}
