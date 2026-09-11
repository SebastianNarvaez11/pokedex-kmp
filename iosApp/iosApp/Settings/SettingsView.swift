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
    @State private var permiso: EstadoDelPermiso = .sinPreguntar
    @State private var pendientes = 0
    @State private var auth: AuthViewModel?
    @State private var sesion: Session?
    @State private var confirmandoSalida = false
    @State private var confirmandoBorrado = false
    @State private var cuenta: AccountViewModel?

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

                Section {
                    Button {
                        Task {
                            if permiso == .concedido {
                                await RecordatorioDiario.programar()
                                pendientes = await RecordatorioDiario.pendientes()
                            } else {
                                permiso = await RecordatorioDiario.pedirPermiso()
                                if permiso == .concedido {
                                    await RecordatorioDiario.programar()
                                    pendientes = await RecordatorioDiario.pendientes()
                                }
                            }
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

                if let sesion {
                    Section {
                        LabeledContent("Correo", value: sesion.email ?? "Sesión iniciada")
                        Button("Cerrar sesión", role: .destructive) {
                            confirmandoSalida = true
                        }
                        // Eliminar la cuenta es obligatorio en las dos tiendas
                        // si la app deja crearla. Sin esta opción, la revisión
                        // la rechaza.
                        Button("Eliminar mi cuenta", role: .destructive) {
                            confirmandoBorrado = true
                        }
                    } header: {
                        Text("Cuenta")
                    } footer: {
                        Text("Tus favoritos se guardan en este dispositivo y seguirán aquí cuando vuelvas a entrar.")
                    }
                }

                Section("Acerca de") {
                    LabeledContent("App", value: "Pokédex")
                    LabeledContent("Datos", value: "PokeAPI")
                }
            }
            .navigationTitle("Ajustes")
            // El diálogo de confirmación de iOS sube desde abajo y marca en
            // rojo la acción destructiva. Es el equivalente de la hoja inferior
            // de Android, y ninguno de los dos se parece al otro.
            .confirmationDialog(
                "¿Cerrar la sesión?",
                isPresented: $confirmandoSalida,
                titleVisibility: .visible
            ) {
                Button("Cerrar sesión", role: .destructive) { auth?.salir() }
                Button("Cancelar", role: .cancel) { }
            } message: {
                Text("Tendrás que volver a entrar para usar la app.")
            }
            .confirmationDialog(
                "¿Eliminar tu cuenta?",
                isPresented: $confirmandoBorrado,
                titleVisibility: .visible
            ) {
                Button("Eliminar", role: .destructive) { cuenta?.borrarCuenta() }
                Button("Cancelar", role: .cancel) { }
            } message: {
                Text("Se borra tu usuario en el servidor y no se puede deshacer. Tus favoritos, que están en este iPhone, no se tocan.")
            }
        }
        .task {
            guard AuthIosKt.hayCuentas() else { return }
            let vm = auth ?? AuthIosKt.authViewModel(owner: owner)
            auth = vm
            cuenta = cuenta ?? AuthIosKt.accountViewModel(owner: owner)
            sesion = vm.authStateForIos.session
            do {
                for try await nuevo in asyncSequence(for: vm.authStateForIosFlow) {
                    sesion = nuevo.session
                }
            } catch { }
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
