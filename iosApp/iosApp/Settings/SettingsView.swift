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
    @State private var permiso: EstadoDelPermiso = .sinPreguntar   // lo que sabe el sistema
    @State private var pendientes = 0                              // avisos programados
    @Environment(\.openURL) private var openURL                    // para abrir los Ajustes de iOS

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    // Lee del estado y escribe en el ViewModel. Las constantes de
                    // un enumerado de Kotlin son objetos unicos que se comparan y
                    // sirven de identificador: el `Picker` usa `Tema` tal cual.
                    Picker("Tema", selection: Binding(
                        get: { ajustes.tema },
                        set: { viewModel?.cambiarTema(tema: $0) }
                    )) {
                        ForEach(Tema.entries, id: \.self) { tema in
                            Text(tema.etiqueta).tag(tema)
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

                Section {
                    Button {
                        Task {
                            switch permiso {
                            case .concedido:
                                await RecordatorioDiario.programar()      // reprogramar
                            case .sinPreguntar:
                                // Pide el permiso y, si lo dan, programa.
                                permiso = await RecordatorioDiario.pedirPermiso()
                                if permiso == .concedido { await RecordatorioDiario.programar() }
                            default:
                                // Denegado: iOS no vuelve a preguntar. Se lleva al usuario a los ajustes.
                                if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                            }
                            pendientes = await RecordatorioDiario.pendientes()
                        }
                    } label: {
                        LabeledContent("Pokémon del día") {
                            switch permiso {
                            case .concedido:
                                Text(pendientes > 0 ? "\(pendientes) días programados" : "Activar")
                            case .sinPreguntar:
                                Text("Activar")
                            default:
                                Text("Desactivadas").foregroundStyle(.secondary)
                            }
                        }
                    }
                } header: {
                    Text("Notificaciones")
                } footer: {
                    Text("Se programan siete días por adelantado. iOS admite 64 notificaciones pendientes por app.")
                }

                Section("Acerca de") {
                    LabeledContent("App", value: "Pokédex")
                    LabeledContent("Datos", value: "PokeAPI")
                }
            }
            .navigationTitle("Ajustes")
        }
        .task {
            permiso = await RecordatorioDiario.estadoDelPermiso()
            pendientes = await RecordatorioDiario.pendientes()
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
