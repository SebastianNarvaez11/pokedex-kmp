import Foundation
import UserNotifications
import Shared

/// El recordatorio diario en iOS.
///
/// La diferencia de fondo con Android: aquí **no hay trabajo en segundo plano**
/// que se ejecute todos los días para decidir qué mostrar. Se programan las
/// notificaciones por adelantado y el sistema las dispara.
///
/// Eso funciona porque el Pokémon del día es una función de la fecha: se puede
/// calcular el de dentro de seis días sin red y sin servidor. Con un contenido
/// que dependiera de una API, esto no se podría hacer.
enum RecordatorioDiario {

    /// Cuántos días se programan por adelantado.
    ///
    /// iOS admite **64 notificaciones pendientes por app**, y a partir de ahí
    /// descarta las más lejanas sin avisar. Siete es de sobra: se reprograman
    /// cada vez que se abre la app.
    static let diasProgramados = 7

    static let identificadorPrefijo = "pokemon-del-dia-"

    static func estadoDelPermiso() async -> EstadoDelPermiso {
        let ajustes = await UNUserNotificationCenter.current().notificationSettings()
        switch ajustes.authorizationStatus {
        case .notDetermined: return .sinPreguntar
        case .authorized, .provisional, .ephemeral: return .concedido
        default: return .denegado
        }
    }

    static func pedirPermiso() async -> EstadoDelPermiso {
        do {
            let concedido = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            return concedido ? .concedido : .denegado
        } catch {
            return .denegado
        }
    }

    /// Programa los próximos días, hora local.
    static func programar(hora: Int = 9) async {
        let centro = UNUserNotificationCenter.current()
        // Se borra lo anterior antes de reprogramar: si no, los días ya
        // programados se acumulan con los nuevos y se llega al límite de 64.
        centro.removePendingNotificationRequests(
            withIdentifiers: (0..<diasProgramados).map { "\(identificadorPrefijo)\($0)" }
        )

        let calendario = Calendar.current
        let hoy = calendario.startOfDay(for: Date())

        for dia in 0..<diasProgramados {
            guard let fecha = calendario.date(byAdding: .day, value: dia, to: hoy) else { continue }
            let componentes = calendario.dateComponents([.year, .month, .day], from: fecha)
            guard let anio = componentes.year, let mes = componentes.month, let dd = componentes.day else { continue }

            // Un puente de tres enteros, para no exportar kotlinx-datetime
            // entera al framework solo por construir una fecha.
            let id = DailyIosKt.pokemonDelDia(anio: Int32(anio), mes: Int32(mes), dia: Int32(dd))

            let contenido = UNMutableNotificationContent()
            contenido.title = "El Pokémon de hoy"
            contenido.body = "Descubre quién es el número \(id)"
            contenido.sound = .default
            // El enlace viaja en el userInfo: al tocar la notificación, la app
            // lo lee y abre la misma ficha que abriría desde fuera.
            contenido.userInfo = ["enlace": DestinationKt.toDeepLink(DestinationDetalle(pokemonId: Int32(id)))]

            var cuando = calendario.dateComponents([.year, .month, .day], from: fecha)
            cuando.hour = hora
            cuando.minute = 0

            // Un trigger por día con `repeats: false`, no uno repetido: si se
            // repitiera, el contenido sería siempre el mismo y el «Pokémon del
            // día» enseñaría el de hoy para siempre.
            let disparo = UNCalendarNotificationTrigger(dateMatching: cuando, repeats: false)

            let peticion = UNNotificationRequest(
                identifier: "\(identificadorPrefijo)\(dia)",
                content: contenido,
                trigger: disparo
            )
            try? await centro.add(peticion)
        }
    }

    static func cancelar() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(
            withIdentifiers: (0..<diasProgramados).map { "\(identificadorPrefijo)\($0)" }
        )
    }

    static func pendientes() async -> Int {
        await UNUserNotificationCenter.current().pendingNotificationRequests()
            .filter { $0.identifier.hasPrefix(identificadorPrefijo) }
            .count
    }
}
