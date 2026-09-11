import Foundation
import UserNotifications

/// Recoge el toque en una notificación y lo convierte en un enlace.
///
/// iOS no entrega el toque por `.onOpenURL`: llega al delegado del centro de
/// notificaciones, que es un objeto aparte. Este publica el enlace y la vista lo
/// observa, para que el camino acabe siendo el mismo que el de un enlace
/// externo: una sola forma de llegar a una pantalla.
final class DelegadoDeNotificaciones: NSObject, ObservableObject, UNUserNotificationCenterDelegate {

    static let compartido = DelegadoDeNotificaciones()

    @Published var enlacePendiente: URL?

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard
            let texto = response.notification.request.content.userInfo["enlace"] as? String,
            let url = URL(string: texto)
        else { return }
        await MainActor.run { enlacePendiente = url }
    }

    /// Sin esto, una notificación que llega con la app abierta no se ve.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
