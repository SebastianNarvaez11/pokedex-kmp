import Foundation
import Shared

/// La configuración de iOS, leída del Info.plist.
///
/// Los valores llegan del `Config.xcconfig`, que no se versiona con las claves
/// dentro. Como en Android, esto no oculta nada de un atacante: lo que evita es
/// tener la clave en el historial de git.
class IosAppConfig: BaseAppConfig {
    override var supabaseUrl: String {
        Bundle.main.object(forInfoDictionaryKey: "SUPABASE_URL") as? String ?? ""
    }

    override var supabaseKey: String {
        Bundle.main.object(forInfoDictionaryKey: "SUPABASE_KEY") as? String ?? ""
    }
}
