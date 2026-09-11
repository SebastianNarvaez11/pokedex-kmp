import XCTest

/// El flujo de sesión **contra un Supabase de verdad**.
///
/// Crea usuarios reales y tarda más de dos minutos, así que **se salta sola**
/// cuando la app no tiene claves de Supabase: sin ellas no hay cuentas que
/// probar. Ese es justo el caso de la verificación continua, que compila sin
/// `Config.local.xcconfig` y ejecuta el esquema entero sin filtro.
///
/// Dos caminos que se probaron y no valen, por si alguien los vuelve a pensar:
///
/// - Un candado por variable de entorno. El proceso de pruebas vive en el
///   simulador y no hereda el entorno de tu terminal. Ni con el prefijo
///   `TEST_RUNNER_`, que es lo que documenta Xcode: `environment` llega vacío.
/// - Excluirla en el `<SkippedTests>` del esquema. Funciona demasiado bien:
///   `-only-testing` **no** levanta esa exclusión, así que la prueba queda
///   inservible incluso pidiéndola por su nombre.
///
/// Para ejecutarla, con claves puestas:
///
///     xcodebuild test -project iosApp/iosApp.xcodeproj -scheme Pokedex \
///       -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
///       -only-testing:PokedexUITests/FlujoDeSesionRealTests
final class FlujoDeSesionRealTests: XCTestCase {

    private var app: XCUIApplication!
    private let password = "pokedex2026"
    private lazy var email = "ios.\(Int(Date().timeIntervalSince1970))@pokedex-kmp.com"

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(es)", "-AppleLocale", "es_ES"]
        app.launch()
    }

    func testElFlujoCompletoDeSesion() throws {
        try limpiarSesionPrevia()

        let enviar = app.buttons["auth-enviar"]
        XCTAssertTrue(enviar.waitForExistence(timeout: 20), "la app deberia abrir en la puerta de sesion")

        // 1 · Registro.
        app.segmentedControls["auth-modo"].buttons["Crear cuenta"].tap()
        rellenar()
        enviar.tap()
        XCTAssertTrue(pestanaAjustes.waitForExistence(timeout: 40), "tras registrarse deberia entrar")

        // 2 · Ajustes enseña el correo que devolvio el servidor.
        pestanaAjustes.tap()
        XCTAssertTrue(apareceElCorreo(timeout: 20), "ajustes deberia mostrar el correo de la sesion")

        // 3 · Cerrar sesion.
        app.buttons["Cerrar sesión"].firstMatch.tap()
        confirmar("Cerrar sesión")
        XCTAssertTrue(enviar.waitForExistence(timeout: 20), "al salir deberia volver a la puerta")

        // 4 · Entrar otra vez: si el servidor acepta, la cuenta existe de verdad.
        rellenar()
        enviar.tap()
        XCTAssertTrue(pestanaAjustes.waitForExistence(timeout: 40), "deberia poder entrar con la cuenta creada")

        // 5 · El token dura 60 s. Pasados 95, la sesion tiene que seguir viva:
        //     eso solo puede pasar si el refresco funciona dentro de la app.
        Thread.sleep(forTimeInterval: 95)
        pestanaAjustes.tap()
        XCTAssertTrue(apareceElCorreo(timeout: 25),
                      "la sesion deberia sobrevivir a la caducidad del token")

        // 6 · Borrar la cuenta.
        borrarCuenta()
        XCTAssertTrue(enviar.waitForExistence(timeout: 30), "al borrar la cuenta deberia volver a la puerta")
    }

    // MARK: - Piezas

    private var pestanaAjustes: XCUIElement { app.tabBars.buttons["Ajustes"] }

    /// Deja la app en la puerta de sesión, y de paso decide si hay algo que
    /// probar.
    ///
    /// Al abrir puede pasar una de tres cosas, y las tres se distinguen aquí:
    ///
    /// 1. Está la puerta de sesión. Perfecto, no hay nada que hacer.
    /// 2. Están las pestañas **y** hay sección de cuenta: quedó la sesión de
    ///    una ejecución anterior, porque la sesión persiste entre arranques.
    ///    Se borra la cuenta, que además la quita del servidor en vez de dejar
    ///    basura acumulándose.
    /// 3. Están las pestañas y **no** hay sección de cuenta: la app se compiló
    ///    sin claves, `hayCuentas()` es falso y no existe la parte de sesión.
    ///    No hay nada que probar y la prueba se salta.
    private func limpiarSesionPrevia() throws {
        guard pestanaAjustes.waitForExistence(timeout: 20) else { return }
        pestanaAjustes.tap()

        let borrar = app.buttons["Eliminar mi cuenta"].firstMatch
        guard borrar.waitForExistence(timeout: 15) else {
            throw XCTSkip("la app se compiló sin claves de Supabase: no hay cuentas que probar")
        }

        borrar.tap()
        confirmar("Eliminar")
        _ = app.buttons["auth-enviar"].waitForExistence(timeout: 30)
    }

    private func borrarCuenta() {
        let borrar = app.buttons["Eliminar mi cuenta"].firstMatch
        guard borrar.waitForExistence(timeout: 15) else { return }
        borrar.tap()
        confirmar("Eliminar")
    }

    /// `LabeledContent("Correo", value: …)` deja el correo en el **valor** del
    /// elemento, no en su rótulo, así que `staticTexts[email]` no lo encuentra.
    private func apareceElCorreo(timeout: TimeInterval) -> Bool {
        let predicado = NSPredicate(format: "label CONTAINS %@ OR value CONTAINS %@", email, email)
        return app.descendants(matching: .any).matching(predicado).firstMatch.waitForExistence(timeout: timeout)
    }

    private func rellenar() {
        let correo = app.textFields["auth-correo"]
        XCTAssertTrue(correo.waitForExistence(timeout: 10))
        correo.tap()
        correo.typeText(email)

        let clave = app.secureTextFields["auth-password"]
        clave.tap()
        descartarContrasenaSegura()
        clave.typeText(password)

        // Un campo seguro se lee como una fila de puntos, uno por carácter. Si
        // iOS se comió parte del texto, el botón de enviar queda deshabilitado
        // y el fallo aparece tres pasos más adelante, donde no se entiende.
        let puntos = (clave.value as? String) ?? ""
        XCTAssertEqual(puntos.count, password.count,
                       "la contrasena no entro entera: '\(puntos)'")
    }

    /// El panel de «¿Usar contraseña segura?» de iOS.
    ///
    /// Lo dispara `.textContentType(.newPassword)`, que la pantalla usa a
    /// propósito: al registrarse conviene que iOS ofrezca una contraseña fuerte
    /// y la guarde en el llavero. El efecto secundario es que al enfocar el
    /// campo se abre una hoja que se traga lo que escribas, y el síntoma no
    /// habla de contraseñas: el botón de enviar sigue deshabilitado.
    private func descartarContrasenaSegura() {
        let cerrar = app.buttons["xmark"]
        if cerrar.waitForExistence(timeout: 3) { cerrar.tap() }
    }

    /// Los `confirmationDialog` de SwiftUI salen como hoja en iOS, y su botón
    /// comparte rótulo con el que la abrió: hay que buscarlo dentro de la hoja.
    private func confirmar(_ titulo: String) {
        let hoja = app.sheets.firstMatch
        if hoja.waitForExistence(timeout: 10) {
            hoja.buttons[titulo].tap()
        } else {
            app.alerts.firstMatch.buttons[titulo].tap()
        }
    }
}
