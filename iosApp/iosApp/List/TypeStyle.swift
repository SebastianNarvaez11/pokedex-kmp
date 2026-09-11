import SwiftUI
import Shared

/// El color y el nombre de cada tipo, para iOS.
///
/// Vive aquí y no en el módulo compartido a propósito: **el aspecto no se
/// comparte**. Android tiene su propia tabla, con sus propios colores, y las
/// dos son correctas. Lo que se comparte es el tipo.
extension PokemonType {

    var tinte: Color {
        switch name {
        case "NORMAL":   return Color(red: 0.56, green: 0.60, blue: 0.63)
        case "FIGHTING": return Color(red: 0.81, green: 0.25, blue: 0.41)
        case "FLYING":   return Color(red: 0.56, green: 0.66, blue: 0.87)
        case "POISON":   return Color(red: 0.67, green: 0.42, blue: 0.78)
        case "GROUND":   return Color(red: 0.85, green: 0.47, blue: 0.27)
        case "ROCK":     return Color(red: 0.78, green: 0.72, blue: 0.55)
        case "BUG":      return Color(red: 0.56, green: 0.76, blue: 0.17)
        case "GHOST":    return Color(red: 0.32, green: 0.41, blue: 0.67)
        case "STEEL":    return Color(red: 0.35, green: 0.56, blue: 0.63)
        case "FIRE":     return Color(red: 1.00, green: 0.62, blue: 0.33)
        case "WATER":    return Color(red: 0.30, green: 0.56, blue: 0.84)
        case "GRASS":    return Color(red: 0.39, green: 0.74, blue: 0.35)
        case "ELECTRIC": return Color(red: 0.96, green: 0.82, blue: 0.24)
        case "PSYCHIC":  return Color(red: 0.98, green: 0.44, blue: 0.46)
        case "ICE":      return Color(red: 0.45, green: 0.81, blue: 0.75)
        case "DRAGON":   return Color(red: 0.04, green: 0.43, blue: 0.76)
        case "DARK":     return Color(red: 0.35, green: 0.33, blue: 0.40)
        case "FAIRY":    return Color(red: 0.93, green: 0.56, blue: 0.90)
        default:         return .gray
        }
    }

    var etiqueta: String {
        switch name {
        case "NORMAL":   return "Normal"
        case "FIGHTING": return "Lucha"
        case "FLYING":   return "Volador"
        case "POISON":   return "Veneno"
        case "GROUND":   return "Tierra"
        case "ROCK":     return "Roca"
        case "BUG":      return "Bicho"
        case "GHOST":    return "Fantasma"
        case "STEEL":    return "Acero"
        case "FIRE":     return "Fuego"
        case "WATER":    return "Agua"
        case "GRASS":    return "Planta"
        case "ELECTRIC": return "Eléctrico"
        case "PSYCHIC":  return "Psíquico"
        case "ICE":      return "Hielo"
        case "DRAGON":   return "Dragón"
        case "DARK":     return "Siniestro"
        case "FAIRY":    return "Hada"
        default:         return name.capitalized
        }
    }
}
