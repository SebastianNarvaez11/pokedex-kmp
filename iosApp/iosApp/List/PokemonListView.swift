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

    private let columnas = [GridItem(.adaptive(minimum: 164), spacing: 12)]

    var body: some View {
        NavigationStack {
            contenido
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
                        PokemonCardView(pokemon: pokemon)
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
