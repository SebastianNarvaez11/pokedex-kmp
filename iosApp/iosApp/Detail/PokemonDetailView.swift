import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// La ficha en iOS.
///
/// El mismo ViewModel de Kotlin que usa Android, y todo lo demás de Apple:
/// scroll con la imagen sangrando hasta arriba, `Gauge` para las estadísticas y
/// el botón de volver que pone el propio `NavigationStack`.
struct PokemonDetailView: View {

    let pokemonId: Int32

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var estado = PokemonDetailUiState(cargando: true, detalle: nil, error: nil)
    @State private var viewModel: PokemonDetailViewModel?

    var body: some View {
        contenido
            .task {
                let vm = viewModel ?? DetailIosKt.pokemonDetailViewModel(owner: owner, pokemonId: pokemonId)
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
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = estado.error {
            VStack(spacing: 10) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.largeTitle)
                    .foregroundStyle(.secondary)
                Text(error.titulo).font(.headline)
                Text(error.detalle).font(.subheadline).foregroundStyle(.secondary)
                if error.sePuedeReintentar {
                    Button("Reintentar") { viewModel?.reintentar() }
                        .buttonStyle(.borderedProminent)
                }
            }
            .padding(32)
        } else if let detalle = estado.detalle {
            Ficha(detalle: detalle)
        }
    }
}

private struct Ficha: View {
    let detalle: PokemonDetail

    private var tinte: Color { detalle.types.first?.tinte ?? .accentColor }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                ZStack(alignment: .topTrailing) {
                    LinearGradient(
                        colors: [tinte.opacity(0.45), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    AsyncImage(url: URL(string: detalle.artworkUrl)) { imagen in
                        imagen.resizable().scaledToFit()
                    } placeholder: {
                        ProgressView()
                    }
                    .padding(42)

                    Text(String(format: "N.º %04d", detalle.id))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.secondary)
                        .padding(20)
                }
                .aspectRatio(1, contentMode: .fit)

                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 10) {
                            Text(detalle.name.capitalized)
                                .font(.system(.largeTitle, design: .rounded, weight: .bold))
                            if detalle.isLegendary {
                                Text("Legendario")
                                    .font(.caption2.weight(.bold))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(.tint.opacity(0.18), in: Capsule())
                            }
                        }
                        if !detalle.genus.isEmpty {
                            Text(detalle.genus).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }

                    HStack(spacing: 8) {
                        ForEach(detalle.types, id: \.name) { tipo in
                            HStack(spacing: 6) {
                                Circle().fill(tipo.tinte).frame(width: 8, height: 8)
                                Text(tipo.etiqueta).font(.callout.weight(.medium))
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 7)
                            .background(tipo.tinte.opacity(0.20), in: Capsule())
                        }
                    }

                    if !detalle.description_.isEmpty {
                        Text(detalle.description_).font(.body)
                    }

                    HStack(spacing: 12) {
                        Medida(titulo: "Altura", valor: String(format: "%.1f m", Double(detalle.heightCm) / 100))
                        Medida(titulo: "Peso", valor: String(format: "%.1f kg", Double(detalle.weightG) / 1000))
                    }

                    if !detalle.stats.isEmpty {
                        Text("Estadísticas base")
                            .font(.system(.headline, design: .rounded, weight: .bold))
                        VStack(spacing: 10) {
                            ForEach(detalle.stats, id: \.kind.name) { stat in
                                HStack(spacing: 12) {
                                    Text(stat.kind.etiqueta)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                        .frame(width: 96, alignment: .leading)
                                    Text("\(stat.value)")
                                        .font(.callout.weight(.semibold))
                                        // Dígitos de ancho fijo: sin esto las
                                        // barras bailan al cambiar de fila.
                                        .monospacedDigit()
                                        .frame(width: 34, alignment: .trailing)
                                    // linearCapacity y no accessoryLinear: el
                                    // primero rellena la barra hasta el valor,
                                    // que es lo que se quiere comparar. El
                                    // segundo pinta la barra entera con una
                                    // marca, y todas parecen llenas.
                                    Gauge(value: Double(stat.value), in: 0...Double(PokemonStat.companion.MAXIMO)) {
                                        EmptyView()
                                    }
                                    .gaugeStyle(.linearCapacity)
                                    .labelsHidden()
                                    .tint(tinte)
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 22)
                .padding(.bottom, 32)
            }
        }
        .ignoresSafeArea(edges: .top)
        .navigationTitle(detalle.name.capitalized)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                // `ShareLink` es una vista, no una acción: SwiftUI monta la
                // hoja de compartir del sistema con solo darle el enlace. En
                // Android hay que construir el Intent y pedir el selector.
                //
                // Se comparte el enlace https y no el pokedex://, porque los
                // esquemas propios no se convierten en enlace pulsable en la
                // mayoría de apps de mensajería.
                // `DestinationKt.toShareUrl(_:)` y no `destino.toShareUrl()`:
                // una funcion de extension de Kotlin llega a Swift como metodo
                // estatico de la clase del fichero, con el receptor como primer
                // argumento. No se puede llamar con notacion de punto.
                ShareLink(
                    item: URL(string: DestinationKt.toShareUrl(DestinationDetalle(pokemonId: detalle.id)))!,
                    subject: Text(detalle.name.capitalized),
                    message: Text("Mira este Pokémon")
                )
            }
        }
    }
}

private struct Medida: View {
    let titulo: String
    let valor: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(titulo).font(.caption).foregroundStyle(.secondary)
            Text(valor).font(.headline)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

extension StatKind {
    var etiqueta: String {
        switch name {
        case "HP": return "PS"
        case "ATTACK": return "Ataque"
        case "DEFENSE": return "Defensa"
        case "SPECIAL_ATTACK": return "At. especial"
        case "SPECIAL_DEFENSE": return "Def. especial"
        case "SPEED": return "Velocidad"
        default: return name.capitalized
        }
    }
}
