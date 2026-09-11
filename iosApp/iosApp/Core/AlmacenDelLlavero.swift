import Foundation
import Security
import Shared

/// El Llavero de iOS, implementando una interfaz que está escrita en Kotlin.
///
/// Esto es lo que hace posible compartir la lógica sin renunciar a lo que solo
/// tiene cada sistema. `SecureStorage` es una interfaz de `commonMain`; el
/// núcleo la pide y no le importa quién la cumpla. En Android la cumple una
/// clase Kotlin y aquí la cumple Swift, con la API del sistema y sin envoltorio
/// de por medio.
///
/// Y es de las pocas veces que la dependencia va al revés: normalmente Swift
/// llama a Kotlin.
final class AlmacenDelLlavero: SecureStorage {

    /// Todo lo que guarde la app comparte este servicio, que es lo que permite
    /// borrarlo junto y no pisar lo de otras apps.
    private let servicio = Bundle.main.bundleIdentifier ?? "com.sebastiannarvaez.pokedex"

    func leer(clave: String) -> String? {
        var consulta = base(clave)
        consulta[kSecReturnData as String] = true
        consulta[kSecMatchLimit as String] = kSecMatchLimitOne

        var resultado: CFTypeRef?
        let estado = SecItemCopyMatching(consulta as CFDictionary, &resultado)
        guard estado == errSecSuccess, let datos = resultado as? Data else { return nil }
        return String(data: datos, encoding: .utf8)
    }

    func guardar(clave: String, valor: String) {
        // El Llavero no tiene «guardar o actualizar»: hay que borrar y añadir,
        // o `SecItemAdd` devuelve -25299 (errSecDuplicateItem) la segunda vez.
        // Ese es el error que convierte «entré dos veces» en «no me guarda».
        borrar(clave: clave)

        var item = base(clave)
        item[kSecValueData as String] = Data(valor.utf8)
        // Sin esta línea, el elemento sale en la copia de seguridad de iCloud y
        // viaja a otros dispositivos. Un token de sesión no debe viajar.
        // `ThisDeviceOnly` además lo ata a este teléfono.
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

        SecItemAdd(item as CFDictionary, nil)
    }

    func borrar(clave: String) {
        SecItemDelete(base(clave) as CFDictionary)
    }

    private func base(_ clave: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: servicio,
            kSecAttrAccount as String: clave,
        ]
    }
}
