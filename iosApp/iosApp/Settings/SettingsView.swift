import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// Los ajustes en iOS.
///
/// `Form` con secciones es el lenguaje de Apple para esto: agrupa, pone los
/// separadores, respeta el tamaño de letra del sistema y en iPad se adapta
/// solo. El selector de tema es un `Picker`, que en un `Form` se pinta como
/// fila con submenú, no como hoja inferior.
struct SettingsView: View {

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var ajustes = Settings(tema: Tema.sistema, favoritosPorNumero: false)
    @State private var viewModel: SettingsViewModel?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Tema", selection: Binding(
                        get: { ajustes.tema.name },
                        set: { viewModel?.cambiarTema(tema: SettingsIosKt.temaDesdeNombre(nombre: $0)) }
                    )) {
                        ForEach(Tema.entries, id: \.name) { tema in
                            Text(tema.etiqueta).tag(tema.name)
                        }
                    }
                } header: {
                    Text("Apariencia")
                } footer: {
                    Text("«Lo que diga el sistema» sigue el ajuste de iOS.")
                }

                Section("Favoritos") {
                    Toggle("Ordenar por número", isOn: Binding(
                        get: { ajustes.favoritosPorNumero },
                        set: { viewModel?.cambiarOrden(porNumero: $0) }
                    ))
                }

                Section("Acerca de") {
                    LabeledContent("App", value: "Pokédex")
                    LabeledContent("Datos", value: "PokeAPI")
                }
            }
            .navigationTitle("Ajustes")
        }
        .task {
            let vm = viewModel ?? SettingsIosKt.settingsViewModel(owner: owner)
            viewModel = vm
            ajustes = vm.settingsForIos
            do {
                for try await nuevo in asyncSequence(for: vm.settingsForIosFlow) {
                    ajustes = nuevo
                }
            } catch {
                // Cancelar no es fallar.
            }
        }
    }
}

extension Tema {
    var etiqueta: String {
        switch name {
        case "CLARO": return "Claro"
        case "OSCURO": return "Oscuro"
        default: return "Lo que diga el sistema"
        }
    }

    /// El esquema que pide SwiftUI. `nil` significa «el del sistema».
    var esquema: ColorScheme? {
        switch name {
        case "CLARO": return .light
        case "OSCURO": return .dark
        default: return nil
        }
    }
}
