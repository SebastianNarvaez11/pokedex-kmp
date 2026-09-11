import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

/// La puerta de la app, en iOS.
///
/// Hace lo mismo que su equivalente de Android y no se parece en nada, que es
/// justo el motivo de escribir dos interfaces. Aquí el selector es un `Picker`
/// segmentado, el error se pregunta con una alerta en vez de quedarse en la
/// pantalla, y los campos declaran `textContentType`, con lo que el Llavero de
/// iCloud ofrece la contraseña guardada y propone una fuerte al registrarse.
/// Nada de eso hay que programarlo.
struct AuthView: View {

    let viewModel: AuthViewModel

    // `AuthFormState()` y no la lista entera de campos: los argumentos por
    // defecto de Kotlin no cruzan, así que el núcleo declara un constructor
    // vacío a propósito. Sin él, cada campo nuevo rompería este fichero.
    @State private var formulario = AuthFormState()
    @State private var registrando = false
    @State private var mostrandoError = false
    @State private var mostrandoCorreoEnviado = false

    /// Mover el foco de correo a contraseña con la tecla «siguiente» del
    /// teclado. Sin esto hay que tocar el segundo campo con el dedo.
    @FocusState private var campo: Campo?

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: registrando ? "person.badge.plus" : "person.crop.circle")
                .font(.system(size: 56))
                .foregroundStyle(.tint)
                // El símbolo rebota al cambiar de modo. Es un detalle de tres
                // palabras que solo existe en iOS.
                .symbolEffect(.bounce, value: registrando)

            VStack(spacing: 6) {
                Text("Pokédex")
                    .font(.largeTitle.bold())
                Text("Entra para guardar tus favoritos y recibir el Pokémon del día.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            Picker("", selection: $registrando) {
                Text("Entrar").tag(false)
                Text("Crear cuenta").tag(true)
            }
            .pickerStyle(.segmented)

            VStack(spacing: 12) {
                TextField("Correo", text: Binding(
                    get: { formulario.email },
                    set: { viewModel.escribirEmail(valor: $0) }
                ))
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($campo, equals: .correo)
                .submitLabel(.next)
                .onSubmit { campo = .password }

                SecureField("Contraseña", text: Binding(
                    get: { formulario.password },
                    set: { viewModel.escribirPassword(valor: $0) }
                ))
                // `.newPassword` al registrarse hace que iOS proponga una
                // contraseña fuerte y la guarde. `.password` al entrar hace que
                // ofrezca la que ya tiene.
                .textContentType(registrando ? .newPassword : .password)
                .focused($campo, equals: .password)
                .submitLabel(.go)
                .onSubmit(enviar)
            }
            .textFieldStyle(.roundedBorder)

            Button(action: enviar) {
                if formulario.enviando {
                    ProgressView().tint(.white)
                } else {
                    Text(registrando ? "Crear cuenta" : "Entrar")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(!formulario.sePuedeEnviar)

            // Recuperar solo tiene sentido al entrar: quien se registra no
            // tiene contraseña que olvidar.
            if !registrando {
                Button("¿Olvidaste tu contraseña?") {
                    campo = nil
                    viewModel.recuperar()
                }
                .font(.footnote)
                .disabled(!formulario.emailValido || formulario.enviando)
            }

            Spacer()
            Spacer()
        }
        .padding(.horizontal, 32)
        // El error se pregunta, no se queda en la pantalla. En Android se pinta
        // un aviso encima del botón; aquí la alerta es lo que el usuario espera
        // cuando una acción suya falla.
        .alert(
            formulario.error?.titulo ?? "",
            isPresented: $mostrandoError,
            presenting: formulario.error
        ) { _ in
            Button("Entendido", role: .cancel) { viewModel.descartarError() }
        } message: { error in
            Text(error.detalle)
        }
        // El aviso no dice si el correo existe. Decirlo le contaría a cualquiera
        // quién tiene cuenta aquí, y Supabase responde igual en los dos casos.
        .alert("Revisa tu correo", isPresented: $mostrandoCorreoEnviado) {
            Button("Entendido", role: .cancel) { }
        } message: {
            Text("Si hay una cuenta con ese correo, le hemos mandado un enlace para poner una contraseña nueva.")
        }
        .onChange(of: formulario.error) { _, nuevo in
            mostrandoError = nuevo != nil
        }
        .onChange(of: formulario.correoEnviado) { _, enviado in
            mostrandoCorreoEnviado = enviado
        }
        .task {
            formulario = viewModel.authFormForIos
            do {
                for try await nuevo in asyncSequence(for: viewModel.authFormForIosFlow) {
                    formulario = nuevo
                }
            } catch { }
        }
    }

    private func enviar() {
        guard formulario.sePuedeEnviar else { return }
        campo = nil
        if registrando {
            viewModel.registrar()
        } else {
            viewModel.entrar()
        }
    }

    private enum Campo: Hashable {
        case correo, password
    }
}
