import Foundation
import Shared

// Todo este fichero vive solo en compilaciones de depuración. En release no se
// compila, así que no hay forma de que una doble acabe en la App Store.
#if DEBUG

/// Las dobles que usan **solo** las pruebas de interfaz, activadas con el
/// argumento de lanzamiento `-uiTests`.
///
/// Por qué existen. `hayCuentas()` devuelve `AppConfig.haySupabase`, que es
/// falso cuando las claves están vacías: sin claves, la app no tiene puerta de
/// sesión. Eso hacía que las pruebas de interfaz dependieran de si tu
/// `Config.local.xcconfig` tenía claves o no — pasaban en un portátil recién
/// clonado y fallaban en el de al lado, con el mismo commit.
///
/// Y lo peor no era la inconsistencia. La verificación continua nunca tiene
/// claves, así que iba en verde probando **una configuración que ninguna app
/// publicada va a tener**: la que no pide entrar. Con estas dobles, las pruebas
/// corren siempre contra la app con sesión, que es la de verdad, y sin tocar la
/// red.
enum Pruebas {
    static var activas: Bool {
        ProcessInfo.processInfo.arguments.contains("-uiTests")
    }
}

/// Una configuración con claves de mentira: lo único que importa es que no
/// estén vacías, porque eso es lo que enciende la parte de cuentas.
final class ConfigDePruebas: BaseAppConfig {
    override var supabaseUrl: String { "https://pruebas.supabase.co" }
    override var supabaseKey: String { "sb_publishable_de_pruebas" }
}

/// El almacén seguro, en memoria y con una sesión ya dentro.
///
/// La caducidad se pone muy lejos a propósito: el repositorio refresca según el
/// reloj, y una sesión vencida dispararía una petición de red que en una prueba
/// de interfaz no debería existir.
///
/// El formato del JSON no es libre: lo escribe y lo lee `SecureStorageTokenStore`
/// en `commonMain`, así que si cambian los nombres de esos campos, esto deja de
/// valer y las pruebas arrancan sin sesión.
final class AlmacenDePruebas: SecureStorage {

    private var valores: [String: String]

    init() {
        let caduca = Int(Date().timeIntervalSince1970) + 60 * 60 * 24 * 365
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

    func leer(clave: String) -> String? { valores[clave] }

    func guardar(clave: String, valor: String) { valores[clave] = valor }

    func borrar(clave: String) { valores.removeValue(forKey: clave) }
}

#endif
