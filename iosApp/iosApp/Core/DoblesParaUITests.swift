import Foundation
import Shared

// `#if DEBUG` es una compilación condicional: lo de dentro solo existe en las
// compilaciones de depuración (las de Run y Test). En la que subes a la App
// Store (Release) este fichero entero desaparece, así que una doble no puede
// acabar publicada.
#if DEBUG

/// ¿Nos ha lanzado una prueba? Lo dice el argumento `-uiTests`, que el test
/// pone al arrancar la app.
enum Pruebas {
    static var activas: Bool {
        ProcessInfo.processInfo.arguments.contains("-uiTests")
    }
}

/// Claves de mentira. Solo importa que no estén vacías: eso es lo que
/// enciende la parte de cuentas.
final class ConfigDeUITests: BaseAppConfig {
    override var supabaseUrl: String { "https://pruebas.supabase.co" }
    override var supabaseKey: String { "sb_publishable_de_pruebas" }
}

/// Un almacén en memoria con una sesión ya guardada: la app arranca «con la
/// sesión iniciada» sin pasar por el Llavero ni por la red.
final class AlmacenDeUITests: SecureStorage {

    private var valores: [String: String]

    init() {
        // Caduca dentro de un año: una sesión vencida haría que la app saliera
        // a la red a refrescarla, y estas pruebas no tocan la red.
        let caduca = Int(Date().timeIntervalSince1970) + 60 * 60 * 24 * 365
        // Este JSON lo lee el módulo compartido: los nombres de los campos
        // tienen que ser exactamente estos. Es una cadena de varias líneas
        // (`"""`); la `\` al final de cada línea significa «no cuentes este
        // salto de línea», así el JSON queda en una sola línea.
        valores = [
            "sesion": """
            {"acceso":"acceso-de-pruebas",\
            "refresco":"refresco-de-pruebas",\
            "caducaEn":\(caduca),\
            "usuario":"u-pruebas",\
            "email":"entrenador@pueblo-paleta.test"}
            """,
        ]
    }

    // Las tres funciones de la interfaz `SecureStorage`, sobre un diccionario.
    func leer(clave: String) -> String? { valores[clave] }
    func guardar(clave: String, valor: String) { valores[clave] = valor }
    func borrar(clave: String) { valores.removeValue(forKey: clave) }
}

#endif
