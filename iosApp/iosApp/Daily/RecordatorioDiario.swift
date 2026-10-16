import Foundation
import UserNotifications   // el centro de notificaciones de iOS
import Shared              // el framework de Kotlin

/// El recordatorio diario en iOS.
///
/// La diferencia de fondo con Android: aquí no hay trabajo en segundo plano que
/// se ejecute cada día. Se programan los avisos por adelantado y el sistema los
/// dispara, con el contenido ya escrito.
enum RecordatorioDiario {

    /// Cuántos días se programan por adelantado. iOS guarda como mucho 64
    /// notificaciones pendientes por app; siete queda lejos.
    static let diasProgramados = 7

    /// Los identificadores son fijos: pokemon-del-dia-0 … pokemon-del-dia-6.
    static let identificadorPrefijo = "pokemon-del-dia-"

    /// ¿En qué estado está el permiso? (el de iOS tiene más casos que el nuestro)
    static func estadoDelPermiso() async -> EstadoDelPermiso {
        let ajustes = await UNUserNotificationCenter.current().notificationSettings()
        switch ajustes.authorizationStatus {
        case .notDetermined: return .sinPreguntar                      // nunca se ha preguntado
        // .provisional: entrega sin molestar, sin haber enseñado el diálogo.
        // .ephemeral: permiso temporal que iOS da en situaciones especiales. Aquí también es «sí».
        case .authorized, .provisional, .ephemeral: return .concedido
        default: return .denegado                                      // .denied: iOS no vuelve a preguntar
        }
    }

    /// Enseña el diálogo del sistema (solo la primera vez) y devuelve el resultado.
    static func pedirPermiso() async -> EstadoDelPermiso {
        do {
            let concedido = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            return concedido ? .concedido : .denegado
        } catch {
            return .denegado
        }
    }

    /// El contenido de la notificación de un día: el Pokémon y el enlace a su ficha.
    private static func contenido(anio: Int, mes: Int, dia: Int) -> UNMutableNotificationContent {
        // El puente de tres enteros (Swift ve los Int de Kotlin como Int32).
        let id = DailyIosKt.pokemonDelDia(anio: Int32(anio), mes: Int32(mes), dia: Int32(dia))

        let contenido = UNMutableNotificationContent()
        contenido.title = "El Pokémon de hoy"
        contenido.body = "Descubre quién es el número \(id)"
        contenido.sound = .default
        // El enlace viaja en el userInfo (un diccionario libre): al tocar la
        // notificación, la app lo lee y abre la misma ficha que abriría desde fuera.
        contenido.userInfo = ["enlace": DestinationKt.toDeepLink(DestinationDetalle(pokemonId: Int32(id)))]
        return contenido
    }

    /// Programa los próximos días, a la hora local indicada.
    static func programar(hora: Int = 9) async {
        let centro = UNUserNotificationCenter.current()
        // Se borra lo anterior antes de reprogramar: si no, los días ya
        // programados se acumulan con los nuevos y se llega al límite.
        centro.removePendingNotificationRequests(
            withIdentifiers: (0..<diasProgramados).map { "\(identificadorPrefijo)\($0)" }
        )

        let calendario = Calendar.current
        let hoy = calendario.startOfDay(for: Date())    // hoy a las 00:00

        for dia in 0..<diasProgramados {
            // La fecha de «hoy + dia días».
            guard let fecha = calendario.date(byAdding: .day, value: dia, to: hoy) else { continue }

            // Año, mes, día y la hora pedida: cuándo debe sonar.
            var cuando = calendario.dateComponents([.year, .month, .day], from: fecha)
            cuando.hour = hora
            cuando.minute = 0
            // `Date()` es «ahora». Si ese momento ya pasó (hoy, después de las 9),
            // se salta este día. La coma del `guard` es «y además»: las dos condiciones
            // tienen que cumplirse, y `momento > Date()` compara dos fechas (mayor = más tarde).
            guard let momento = calendario.date(from: cuando), momento > Date() else { continue }

            // Los tres números de la fecha, para pedir el contenido de ese día.
            guard let anio = cuando.year, let mes = cuando.month, let diaDelMes = cuando.day else { continue }

            // Un disparador por día con `repeats: false`, no uno repetido: si se
            // repitiera, el contenido sería siempre el mismo y el «Pokémon del
            // día» enseñaría el de hoy para siempre.
            let disparo = UNCalendarNotificationTrigger(dateMatching: cuando, repeats: false)

            let peticion = UNNotificationRequest(
                identifier: "\(identificadorPrefijo)\(dia)",
                content: contenido(anio: anio, mes: mes, dia: diaDelMes),
                trigger: disparo
            )
            try? await centro.add(peticion)   // entrega la petición al sistema
        }
    }

    /// Cuántos avisos del Pokémon del día quedan pendientes.
    static func pendientes() async -> Int {
        await UNUserNotificationCenter.current().pendingNotificationRequests()
            .filter { $0.identifier.hasPrefix(identificadorPrefijo) }
            .count
    }
}
