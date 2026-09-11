import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// Los favoritos en iOS.
///
/// Los dos gestos son los de Apple y no se parecen a los de Android: acciones
/// al deslizar con `.swipeActions`, que además dejan asomar el botón con su
/// icono, y un diálogo de confirmación en vez de una hoja inferior, porque en
/// iOS lo destructivo se confirma con un `confirmationDialog`.
struct FavoritesView: View {

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var estado = FavoritesUiState(favoritos: [], cargando: true)
    @State private var viewModel: FavoritesViewModel?
    @State private var confirmandoVaciar = false
    @Binding var camino: [Int32]

    var body: some View {
        NavigationStack(path: $camino) {
            contenido
                .navigationDestination(for: Int32.self) { id in
                    PokemonDetailView(pokemonId: id)
                }
                .navigationTitle("Favoritos")
                .toolbar {
                    if !estado.favoritos.isEmpty {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("Vaciar", systemImage: "trash") { confirmandoVaciar = true }
                        }
                    }
                }
                .confirmationDialog(
                    "¿Quitar todos los favoritos?",
                    isPresented: $confirmandoVaciar,
                    titleVisibility: .visible
                ) {
                    Button("Quitar todos", role: .destructive) { viewModel?.vaciar() }
                    Button("Cancelar", role: .cancel) { }
                } message: {
                    Text("Se guardan solo en este dispositivo y no se pueden recuperar.")
                }
        }
        .task {
            let vm = viewModel ?? FavoritesIosKt.favoritesViewModel(owner: owner)
            viewModel = vm
            estado = vm.uiStateForIos
            do {
                for try await nuevo in asyncSequence(for: vm.uiStateForIosFlow) {
                    estado = nuevo
                }
            } catch {
                // Cancelar no es fallar.
            }
        }
    }

    @ViewBuilder
    private var contenido: some View {
        if estado.cargando {
            ProgressView()
        } else if estado.vacia {
            ContentUnavailableView(
                "Todavía no hay favoritos",
                systemImage: "heart",
                description: Text("Toca el corazón de cualquier Pokémon para guardarlo aquí.")
            )
        } else {
            List {
                ForEach(estado.favoritos, id: \.id) { favorito in
                    Button {
                        camino.append(favorito.id)
                    } label: {
                        HStack(spacing: 16) {
                            AsyncImage(url: URL(string: favorito.artworkUrl)) { imagen in
                                imagen.resizable().scaledToFit()
                            } placeholder: {
                                Color.clear
                            }
                            .frame(width: 52, height: 52)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(favorito.name.capitalized)
                                    .font(.system(.headline, design: .rounded))
                                HStack(spacing: 8) {
                                    Text(numeroDePokedex(favorito.id))
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                    // El tipo viaja con el favorito: la fila se
                                    // pinta entera sin red.
                                    if let tipo = favorito.primaryType {
                                        HStack(spacing: 5) {
                                            Circle().fill(tipo.tinte).frame(width: 7, height: 7)
                                            Text(tipo.etiqueta)
                                                .font(.caption2)
                                                .foregroundStyle(.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    // Sin esto, VoiceOver lee la fila en tres trozos sueltos:
                    // el nombre, el número y el tipo. `.combine` los junta en
                    // una sola frase, que es como se lee una fila.
                    .accessibilityElement(children: .combine)
                    // `.swipeActions` deja ver el botón con su icono mientras
                    // se desliza, y permite varias acciones por lado. En
                    // Android el equivalente pinta un fondo y hay que dibujarlo.
                    .swipeActions(edge: .trailing) {
                        Button("Quitar", systemImage: "heart.slash", role: .destructive) {
                            viewModel?.toggle(id: favorito.id, name: favorito.name, primaryType: favorito.primaryType)
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }
}
