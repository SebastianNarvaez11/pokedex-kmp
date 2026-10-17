import XCTest

/// Los tests de interfaz de iOS.
///
/// A diferencia de los de Android, estos **sí** arrancan la app de verdad en un
/// simulador: `XCUIApplication` la lanza como la lanzaría una persona y la
/// maneja desde fuera del proceso. Son más lentos y más fieles.
///
/// Por eso cubren lo que los otros no pueden: que las pestañas cambien de
/// verdad, que el sistema pinte el `Form`, que los textos quepan.
///
/// Ninguno depende de PokeAPI. Un test de interfaz que necesita red falla el
/// día que la red va lenta, y entonces deja de creerse.
final class PokedexUITests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        // `-uiTests` activa las dobles (DoblesParaUITests.swift): la app arranca
        // con una sesion de mentira y sin depender de las claves de Supabase.
        // Los otros cuatro fijan el idioma y el formato de la app. Sin ellos,
        // los tests corren en el del simulador y los que buscan «Ajustes»
        // encuentran «Settings».
        app.launchArguments = ["-uiTests", "-AppleLanguages", "(es)", "-AppleLocale", "es_ES"]
        app.launch()
    }

    func testLaAppAbreConLasTresPestanas() {
        let pestanas = app.tabBars.firstMatch
        XCTAssertTrue(pestanas.waitForExistence(timeout: 20))

        XCTAssertTrue(pestanas.buttons["Pokédex"].exists)
        XCTAssertTrue(pestanas.buttons["Favoritos"].exists)
        XCTAssertTrue(pestanas.buttons["Ajustes"].exists)
    }

    func testFavoritosVacioExplicaQueHacer() {
        app.tabBars.buttons["Favoritos"].tap()

        let explicacion = app.staticTexts["Toca el corazón de cualquier Pokémon para guardarlo aquí."]
        XCTAssertTrue(explicacion.waitForExistence(timeout: 10))
    }

    func testAjustesMuestraElTemaYLoCambia() {
        app.tabBars.buttons["Ajustes"].tap()

        let tema = app.buttons["ajustes.tema"]
        XCTAssertTrue(tema.waitForExistence(timeout: 20))
        tema.tap()

        // El `Picker` dentro de un `Form` se abre como lista de opciones. Es un
        // detalle del sistema, no algo que escribamos: en Android la misma
        // elección se pide con una hoja inferior.
        let oscuro = app.buttons["Oscuro"]
        XCTAssertTrue(oscuro.waitForExistence(timeout: 5))
        oscuro.tap()

        // La etiqueta del selector lleva el valor dentro: «Tema, Oscuro».
        XCTAssertTrue(app.buttons["ajustes.tema"].label.contains("Oscuro"))
    }

    /// El ajuste tiene que sobrevivir a cerrar la app: si no, DataStore no está
    /// escribiendo y nadie se entera hasta que un usuario lo cuenta.
    func testElTemaSobreviveAReabrirLaApp() {
        app.tabBars.buttons["Ajustes"].tap()
        let tema = app.buttons["ajustes.tema"]
        XCTAssertTrue(tema.waitForExistence(timeout: 20))
        tema.tap()
        // Espera a que la lista de opciones aparezca antes de tocar «Oscuro».
        let oscuro = app.buttons["Oscuro"]
        XCTAssertTrue(oscuro.waitForExistence(timeout: 5))
        oscuro.tap()

        // Mata el proceso y vuelve a arrancarlo. Los argumentos de lanzamiento
        // se conservan, así que la app arranca con las mismas dobles.
        app.terminate()
        app.launch()
        app.tabBars.buttons["Ajustes"].tap()

        let vuelta = app.buttons["ajustes.tema"]
        XCTAssertTrue(vuelta.waitForExistence(timeout: 20))
        XCTAssertTrue(vuelta.label.contains("Oscuro"))
    }
}
