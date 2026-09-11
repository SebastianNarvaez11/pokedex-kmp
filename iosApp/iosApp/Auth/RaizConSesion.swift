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
            viewModel?.abrirRecuperacion(enlace: enlace)
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
            do {
                for try await nuevo in asyncSequence(for: vm.authStateForIosFlow) {
                    estado = nuevo
                }
            } catch { }
        }
    }
}
