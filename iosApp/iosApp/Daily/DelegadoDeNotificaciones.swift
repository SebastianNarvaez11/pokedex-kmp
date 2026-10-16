import Foundation
import UserNotifications

/// Recoge el toque en una notificación y lo convierte en un enlace.
///
/// No navega: publica el enlace y la vista lo observa, para que el camino sea el
/// mismo que el de un enlace externo. Una sola forma de llegar a una pantalla.
final class DelegadoDeNotificaciones: NSObject, ObservableObject, UNUserNotificationCenterDelegate {

    /// Una sola instancia, para que la app y la vista hablen con la misma.
    static let compartido = DelegadoDeNotificaciones()

    /// El enlace que acaba de llegar; la vista lo lee y lo pone a nil.
    @Published var enlacePendiente: URL?

    /// iOS llama aquí cuando el usuario toca la notificación.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard
            let texto = response.notification.request.content.userInfo["enlace"] as? String,
            let url = URL(string: texto)
        else { return }
        await MainActor.run { enlacePendiente = url }   // publicar desde el hilo principal
    }

    /// Sin esto, una notificación que llega con la app abierta no se ve
    /// (es el mismo `willPresent` de Marcador).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
