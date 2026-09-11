import Foundation

/// El número de la ficha, ya traducido.
///
/// `String(format:)` **no mira el catálogo**: coge la cadena que le des tal
/// cual. Es el mismo fallo que `Text` con una variable, y aquí se veía en la
/// app en inglés, que seguía enseñando «N.º 0001» con toda la interfaz
/// traducida alrededor.
///
/// `String(localized:)` busca la clave y devuelve el formato del idioma —en
/// inglés, «No. %04d»—, y ese es el que se rellena.
func numeroDePokedex(_ id: Int32) -> String {
    String(format: String(localized: "N.º %04d"), id)
}
