import SwiftUI
import Shared

/// Los resultados de la búsqueda, como lista.
///
/// En Android la barra de Material se expande a pantalla completa y lleva los
/// resultados dentro. Aquí no hace falta: `.searchable` deja el campo en la
/// barra de navegación y la vista decide qué pintar debajo, que es más simple
/// y más de iOS.
struct SearchResultsView: View {

    let estado: SearchUiState
    let alElegir: (Int32) -> Void

    var body: some View {
        if estado.enReposo {
            ContentUnavailableView(
                "Busca un Pokémon",
                systemImage: "magnifyingglass",
                description: Text("Escribe un nombre para empezar")
            )
        } else if estado.buscando && estado.resultados.isEmpty {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = estado.error {
            ContentUnavailableView("No se pudo buscar", systemImage: "wifi.exclamationmark", description: Text(error.detalle))
        } else if estado.sinResultados {
            // ContentUnavailableView es de iOS 17: el estado vacío con su
            // icono, su título y su texto, ya maquetado por el sistema. En
            // Android hay que componerlo a mano.
            ContentUnavailableView.search(text: estado.consulta)
        } else {
            List(estado.resultados, id: \.id) { ref in
                Button {
                    alElegir(ref.id)
                } label: {
                    HStack(spacing: 14) {
                        AsyncImage(url: URL(string: ref.artworkUrl)) { imagen in
                            imagen.resizable().scaledToFit()
                        } placeholder: {
                            Color.clear
                        }
                        .frame(width: 44, height: 44)

                        VStack(alignment: .leading, spacing: 1) {
                            Text(ref.name.capitalized)
                                .font(.body.weight(.medium))
                            Text(numeroDePokedex(ref.id))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .buttonStyle(.plain)
            }
            .listStyle(.plain)
        }
    }
}
