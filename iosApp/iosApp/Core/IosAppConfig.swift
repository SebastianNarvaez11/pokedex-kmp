import Foundation
import Shared

// La configuración de iOS, leída del Info.plist. `BaseAppConfig` es la clase
// de Kotlin del paso 1, y una clase de Swift puede heredar de una de Kotlin:
// llega a Swift como una clase más, de la que se hereda con `:` y a la que se
// sobrescribe con `override`.
class IosAppConfig: BaseAppConfig {
    // Un `val` de Kotlin llega a Swift como una propiedad de solo lectura, y se
    // sobrescribe con una propiedad calculada (`override var` y un cuerpo con
    // llaves): cada vez que se lee, ejecuta el cuerpo. `Bundle.main` es el
    // paquete de la app en marcha; `object(forInfoDictionaryKey:)` lee una
    // clave del Info.plist y la devuelve como `Any?`; `as? String` la convierte
    // a texto y `?? ""` pone un texto vacío si faltaba.
    override var supabaseUrl: String {
        Bundle.main.object(forInfoDictionaryKey: "SUPABASE_URL") as? String ?? ""
    }

    override var supabaseKey: String {
        Bundle.main.object(forInfoDictionaryKey: "SUPABASE_KEY") as? String ?? ""
    }
}
