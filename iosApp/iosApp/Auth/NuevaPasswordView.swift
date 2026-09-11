import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// La contraseña nueva, tras abrir el enlace del correo.
///
/// Va en una `sheet` porque el usuario **ya está dentro**: el enlace trae una
/// sesión temporal. Presentarla como pantalla completa dejaría la app detrás en
/// un estado a medio camino entre fuera y dentro.
struct NuevaPasswordView: View {

    let alTerminar: () -> Void

    @StateObject private var owner = IosViewModelStoreOwner()
    @State private var viewModel: AccountViewModel?
    @State private var estado = AccountState(trabajando: false, error: nil, passwordCambiada: false)
    @State private var password = ""
    @FocusState private var enfocado: Bool

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField("Contraseña", text: $password)
                        .textContentType(.newPassword)
                        .focused($enfocado)
                } header: {
                    Text("Nueva contraseña")
                } footer: {
                    if let error = estado.error {
                        Text(error.detalle).foregroundStyle(.red)
                    } else {
                        Text("Al menos \(AuthFormState.companion.MINIMO_PASSWORD) caracteres.")
                    }
                }
            }
            .navigationTitle("Cambiar contraseña")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Ahora no", action: alTerminar)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") {
                        viewModel?.cambiarPassword(nueva: password)
                    }
                    .disabled(password.count < Int(AuthFormState.companion.MINIMO_PASSWORD) || estado.trabajando)
                }
            }
        }
        .onChange(of: estado.passwordCambiada) { _, cambiada in
            if cambiada { alTerminar() }
        }
        .task {
            enfocado = true
            let vm = viewModel ?? AuthIosKt.accountViewModel(owner: owner)
            viewModel = vm
            estado = vm.accountStateForIos
            do {
                for try await nuevo in asyncSequence(for: vm.accountStateForIosFlow) {
                    estado = nuevo
                }
            } catch { }
        }
    }
}
