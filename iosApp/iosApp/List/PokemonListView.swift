import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// La lista de Pokémon en iOS.
///
/// El estado viene del presentador de Kotlin —el mismo Paging que usa
/// Android— y todo lo demás es SwiftUI puro: `NavigationStack`, `LazyVGrid`,
/// `.refreshable` y SF Symbols.
struct PokemonListView: View {

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var estado = PagedListState(
        items: [], cargando: true, cargandoMas: false, error: nil, errorAlAmpliar: nil
    )
    @State private var presenter: PokemonListPresenter?

    @State private var buscador: PokemonSearchViewModel?
    @State private var busqueda = SearchUiState(
        consulta: "", resultados: [], buscando: false, error: nil
    )
    @State private var texto = ""

    /// La pila, como dato. El equivalente de la lista de Navigation 3, solo
    /// que aquí lo trae el sistema: `NavigationStack` recibe el camino y
    /// `navigationDestination` dice qué pintar en cada paso.
    @State private var camino: [Int32] = []

    private let columnas = [GridItem(.adaptive(minimum: 164), spacing: 12)]

    var body: some View {
        NavigationStack(path: $camino) {
            Group {
                // `.searchable` es un modificador: el campo lo pone el sistema
                // en la barra de navegación y se recoge solo al hacer scroll.
                // En Android la barra de búsqueda es un componente que hay que
                // colocar, y hay que decidir qué pasa con el título grande.
                if texto.isEmpty {
                    contenido
                } else {
                    SearchResultsView(estado: busqueda) { id in
                        texto = ""
                        camino.append(id)
                    }
                }
            }
            .searchable(text: $texto, prompt: "Buscar Pokémon")
            .onChange(of: texto) { _, nuevo in buscador?.escribir(texto: nuevo) }
            .navigationDestination(for: Int32.self) { id in
                PokemonDetailView(pokemonId: id)
            }
            .navigationTitle("Pokédex")
                // El título grande que se encoge al hacer scroll es de iOS, y
                // no es lo mismo que la barra grande de Material: aquí lo pone
                // el sistema y hereda el aspecto de la versión instalada.
                .navigationBarTitleDisplayMode(.large)
        }
        .task {
            let p = presenter ?? ListIosKt.pokemonListPresenter(owner: owner)
            presenter = p
            do {
                for try await nuevo in asyncSequence(for: p.stateFlow) {
                    estado = nuevo
                }
            } catch {
                // Cancelar no es fallar: la vista desapareció.
            }
        }
        .task {
            let vm = buscador ?? SearchIosKt.pokemonSearchViewModel(owner: owner)
            buscador = vm
            do {
                for try await nuevo in asyncSequence(for: vm.uiStateForIosFlow) {
                    busqueda = nuevo
                }
            } catch {
                // Cancelar no es fallar.
            }
        }
    }

    @ViewBuilder
    private var contenido: some View {
        if estado.cargando && estado.items.isEmpty {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = estado.error, estado.items.isEmpty {
            ErrorView(error: error) { presenter?.retry() }
        } else {
            ScrollView {
                LazyVGrid(columns: columnas, spacing: 12) {
                    ForEach(Array(estado.items.enumerated()), id: \.element.id) { indice, pokemon in
                        // Button y no un gesto de toque: así se hereda el
                        // resaltado al pulsar y VoiceOver lo anuncia como algo
                        // pulsable, sin escribir nada.
                        Button { camino.append(pokemon.id) } label: {
                            PokemonCardView(pokemon: pokemon)
                        }
                        .buttonStyle(.plain)
                        // Así se pide la página siguiente: la vista dice por
                        // dónde va mirando y Paging decide si hace falta.
                        .onAppear { presenter?.loadAround(index: Int32(indice)) }
                    }
                }
                .padding(16)

                if estado.cargandoMas {
                    ProgressView().padding(.vertical, 16)
                }

                if let error = estado.errorAlAmpliar {
                    ErrorView(error: error, compacto: true) { presenter?.retry() }
                }
            }
            // El gesto nativo de tirar para refrescar. En Android hay que
            // envolver la lista en un componente; aquí es un modificador.
            .refreshable { presenter?.refresh() }
        }
    }
}

private struct ErrorView: View {
    let error: UiError
    var compacto: Bool = false
    let reintentar: () -> Void

    var body: some View {
        VStack(spacing: 10) {
            if !compacto {
                Image(systemName: "wifi.exclamationmark")
                    .font(.largeTitle)
                    .foregroundStyle(.secondary)
            }
            Text(error.titulo)
                .font(.headline)
            Text(error.detalle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if error.sePuedeReintentar {
                Button("Reintentar", action: reintentar)
                    .buttonStyle(.borderedProminent)
                    .padding(.top, 4)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: compacto ? nil : .infinity)
    }
}
