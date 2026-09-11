import SwiftUI
import Shared

/// La tarjeta de la rejilla, en lenguaje de iOS.
///
/// No imita a la de Android: usa `AsyncImage` del sistema, materiales de
/// Apple y tipografía redondeada. La retícula se parece porque el contenido es
/// el mismo, no porque se comparta código.
struct PokemonCardView: View {

    let pokemon: Pokemon
    let esFavorito: Bool
    let alMarcar: () -> Void

    private var tinte: Color { pokemon.types.first?.tinte ?? .accentColor }

    /// El tamaño de letra que ha elegido la persona en Ajustes.
    ///
    /// Se consulta porque en los tamaños de accesibilidad la tarjeta deja de
    /// funcionar: con el texto al máximo, «Veneno» se partía en cuatro líneas
    /// de dos letras y el nombre se quedaba en «Bul…». Se vio poniendo
    /// `accessibility-extra-extra-extra-large` en el simulador.
    @Environment(\.dynamicTypeSize) private var tamano

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                LinearGradient(
                    colors: [tinte.opacity(0.42), tinte.opacity(0.10)],
                    startPoint: .top,
                    endPoint: .bottom
                )

                AsyncImage(url: URL(string: pokemon.artworkUrl)) { fase in
                    switch fase {
                    case .success(let imagen):
                        imagen.resizable().scaledToFit()
                    case .failure:
                        // SF Symbols: el vocabulario visual del sistema, que en
                        // Android no existe.
                        Image(systemName: "photo.badge.exclamationmark")
                            .font(.title2)
                            .foregroundStyle(.secondary)
                    default:
                        ProgressView()
                    }
                }
                .padding(14)

                HStack {
                    Text(String(format: "N.º %04d", pokemon.id))
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.secondary)
                        // El número es una etiqueta sobre la ilustración: si
                        // crece, la tapa. Apple documenta este tope justo para
                        // estos casos, y VoiceOver sigue leyéndolo igual.
                        .dynamicTypeSize(...DynamicTypeSize.xxLarge)
                    Spacer()
                    // El corazón con su propia zona táctil: marcar no debe
                    // abrir la ficha.
                    Button(action: alMarcar) {
                        Image(systemName: esFavorito ? "heart.fill" : "heart")
                            .font(.subheadline)
                            .foregroundStyle(esFavorito ? Color.red : Color.secondary)
                            .contentTransition(.symbolEffect(.replace))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(esFavorito ? "Quitar de favoritos" : "Añadir a favoritos")
                }
                .padding(12)
            }
            .aspectRatio(1.15, contentMode: .fit)

            VStack(alignment: .leading, spacing: 8) {
                Text(pokemon.name.capitalized)
                    .font(.system(.headline, design: .rounded, weight: .bold))
                    // Dos líneas antes que recortar: «Bul…» no le dice nada a
                    // nadie, y un nombre en dos líneas se lee perfectamente.
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)

                // Las etiquetas bajan de línea en vez de estrecharse. Con
                // `HStack` se repartían el ancho y partían la palabra letra a
                // letra, que es el peor resultado posible.
                if tamano.isAccessibilitySize {
                    VStack(alignment: .leading, spacing: 6) {
                        ForEach(pokemon.types, id: \.name) { tipo in
                            TypeChip(tipo: tipo)
                        }
                    }
                } else {
                    HStack(spacing: 6) {
                        ForEach(pokemon.types, id: \.name) { tipo in
                            TypeChip(tipo: tipo)
                        }
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.top, 10)
            .padding(.bottom, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        // Una sola etiqueta para VoiceOver, en vez de leer número, nombre y
        // tipos como cuatro cosas sueltas.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            pokemon.types.isEmpty
                ? pokemon.name.capitalized
                : "\(pokemon.name.capitalized), tipo \(pokemon.types.map(\.etiqueta).joined(separator: " y "))"
        )
    }
}

private struct TypeChip: View {
    let tipo: PokemonType

    var body: some View {
        HStack(spacing: 5) {
            Circle().fill(tipo.tinte).frame(width: 7, height: 7)
            Text(tipo.etiqueta)
                .font(.caption2.weight(.medium))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .background(tipo.tinte.opacity(0.18), in: Capsule())
    }
}
