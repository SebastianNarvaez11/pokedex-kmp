import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// El guardián, en la raíz y no dentro de la navegación.
///
/// Mientras no haya sesión, `TabView` **no se construye**: no hay pestaña que
/// alcanzar, ni enlace profundo que la abra, ni pila que restaurar. Proteger
/// pantalla por pantalla dejaría siempre alguna fuera.
struct RaizConSesion<Contenido: View>: View {

    @ViewBuilder let contenido: () -> Contenido

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var viewModel: AuthViewModel?
    @State private var estado = AuthState(comprobando: true, session: nil)
    @State private var cambiandoPassword = false
    // El enlace que llegó antes de que exista el ViewModel (arranque en frío).
    @State private var enlacePendiente: EnlaceDeRecuperacion?

    /// Se calcula una sola vez: la configuración no cambia mientras la app vive.
    private let hayCuentas = AuthIosKt.hayCuentas()

    var body: some View {
        Group {
            if !hayCuentas {
                // Sin Supabase configurado la app va directa. Así se puede
                // clonar el repositorio y ejecutarlo sin crear ninguna cuenta.
                contenido()
            } else if estado.comprobando {
                ProgressView()
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if estado.haySesion {
                contenido()
            } else if let viewModel {
                AuthView(viewModel: viewModel)
                    // La transición la pinta el sistema: entrar no debe ser un
                    // corte seco entre dos pantallas.
                    .transition(.opacity)
            }
        }
        .animation(.default, value: estado.haySesion)
        // El enlace de recuperación **también** encaja en el esquema de la app,
        // así que hay que mirarlo antes de que nadie lo lea como pantalla.
        .onOpenURL { url in
            // Ojo con la diferencia: `Destination` es una interfaz sellada y
            // llega a Swift como protocolo, que no puede tener companion, así
            // que el suyo se expone aparte como `DestinationCompanion`. Esto es
            // una clase normal y **sí** conserva el suyo anidado.
            guard let enlace = EnlaceDeRecuperacion.companion.parse(url: url.absoluteString) else { return }
            // Arranque en frío: el ViewModel aún no existe. Se guarda y la
            // `.task` lo atiende. (En el simulador la `.task` suele llegar
            // antes, pero SwiftUI no promete el orden.)
            guard let viewModel else {
                enlacePendiente = enlace
                return
            }
            // Adopta la sesión temporal del enlace y abre la hoja.
            viewModel.abrirRecuperacion(enlace: enlace)
            cambiandoPassword = true
        }
        .sheet(isPresented: $cambiandoPassword) {
            NuevaPasswordView { cambiandoPassword = false }
        }
        .task {
            guard hayCuentas else { return }
            let vm = viewModel ?? AuthIosKt.authViewModel(owner: owner)
            viewModel = vm
            estado = vm.authStateForIos
            // Si llegó un enlace antes de que existiera el ViewModel, se atiende ahora.
            if let enlace = enlacePendiente {
                enlacePendiente = nil
                vm.abrirRecuperacion(enlace: enlace)
                cambiandoPassword = true
            }
            do {
                for try await nuevo in asyncSequence(for: vm.authStateForIosFlow) {
                    estado = nuevo
                }
            } catch { }
        }
    }
}
