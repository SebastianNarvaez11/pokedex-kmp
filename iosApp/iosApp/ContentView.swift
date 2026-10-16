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

    // La pestaña visible. Tiene que ser estado porque un enlace
    // entrante la cambia: si llega una ficha estando en Favoritos, hay que
    // saltar a la Pokédex antes de apilar nada.
    @State private var pestana = Pestana.lista

    // La vista observa al delegado; cuando publica un enlace, se entera.
    @StateObject private var notificaciones = DelegadoDeNotificaciones.compartido
    // Si la app está activa, en segundo plano o inactiva.
    @Environment(\.scenePhase) private var scenePhase

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
        // El toque en una notificación acaba en el mismo sitio que un enlace externo.
        // `initial: true`: si el toque llegó antes de entrar (sin sesión esta vista
        // aún no existía), el delegado lo guardó y se atiende al aparecer.
        .onChange(of: notificaciones.enlacePendiente, initial: true) { _, url in
            guard let url else { return }
            abrir(url)
            notificaciones.enlacePendiente = nil   // ya se atendió
        }
        // Reponer los avisos cada vez que la app pasa a primer plano.
        .onChange(of: scenePhase, initial: true) { _, fase in
            guard fase == .active else { return }
            Task {
                if await RecordatorioDiario.estadoDelPermiso() == .concedido {
                    await RecordatorioDiario.programar()
                }
            }
        }
        // `.onOpenURL` cubre los dos casos, app cerrada y app abierta,
        // sin distinguirlos. En Android son dos sitios (`onCreate` y
        // `onNewIntent`), y olvidar el segundo hace que el enlace falle si la app ya estaba abierta.
        .onOpenURL { url in
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

// Lo que hacen los dos caminos (`.onOpenURL` y el toque), en un solo sitio.
private extension ContentView {
    func abrir(_ url: URL) {
        // La traducción vive en Kotlin; aquí solo se llama. Devuelve nil si
        // no entiende el enlace, y entonces no se hace nada.
        // `DestinationCompanion.shared` y no `Destination.companion`: una interfaz
        // sellada llega a Swift como protocolo, y un protocolo no tiene companion.
        guard let destino = DestinationCompanion.shared.parse(url: url.absoluteString) else { return }
        pestana = .lista   // antes de apilar, la pestaña de la Pokédex
        if let detalle = destino as? DestinationDetalle {   // ¿es una ficha? (`DestinationDetalle` es `Destination.Detalle` de Kotlin)
            caminoLista = [detalle.pokemonId]   // una ficha: el camino es solo ella
        } else {
            caminoLista = []                     // la lista: sin nada apilado
        }
    }
}

#Preview {
    ContentView()
}
