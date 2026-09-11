# Accesibilidad

Lo que hay aquí salió de mirar la app con los ajustes al máximo, no de repasar
el código. Las dos cosas que se rompieron no se ven leyendo.

## Cómo se audita, en dos comandos

```bash
# Android: la letra del sistema al 180 %
adb shell settings put system font_scale 1.8
adb shell settings put system font_scale 1.0   # para dejarlo como estaba

# iOS: el mayor tamaño de accesibilidad
xcrun simctl ui booted content_size accessibility-extra-extra-extra-large
xcrun simctl ui booted content_size large
```

Los dos aceptan la app ya instalada: no hay que compilar nada distinto.

## Lo que se rompió en Android

Con la letra al 180 %, las etiquetas de tipo de la tarjeta se repartían el ancho
y **partían la palabra por la mitad**: «Ven / eno». Un `Row` reparte el espacio
que hay; no baja de línea.

El arreglo es `FlowRow`, que sí baja, más `maxLines = 1` con puntos suspensivos
en la etiqueta. Una palabra recortada se entiende; partida, no.

Lo demás aguantó: la barra de navegación, el título grande y el buscador crecen
sin romperse, porque son componentes de Material 3 y ya lo tienen resuelto.

## Lo que se rompió en iOS, que fue más

Con el tamaño de accesibilidad más grande, la rejilla de dos columnas se
convirtió en algo ilegible:

- «Veneno» bajaba a cuatro líneas de dos letras.
- El nombre quedaba en «Bul…».
- El número de la ficha se salía por encima de la ilustración.

Tres arreglos, todos del lenguaje de SwiftUI:

- **Una sola columna** cuando `dynamicTypeSize.isAccessibilitySize`. Es lo que
  hacen las apps de Apple: con el texto al máximo se renuncia a la retícula
  antes que a que el texto se lea.
- **`.dynamicTypeSize(...DynamicTypeSize.xxLarge)`** en el número, que es una
  etiqueta sobre la imagen y al crecer la tapa. El tope es visual: VoiceOver lo
  sigue leyendo igual.
- **Dos líneas** para el nombre en vez de recortarlo.

## Lo que sí estaba bien, y por qué

**Imágenes decorativas sin descripción.** La ilustración de la tarjeta lleva
`contentDescription = null` a propósito: el nombre está justo al lado.
Describirla haría que el lector dijera «Bulbasaur, imagen de Bulbasaur».

**La tarjeta se lee como una sola cosa.** En iOS con
`.accessibilityElement(children: .ignore)` y una etiqueta compuesta; en Android
el `clickable` de la tarjeta ya fusiona a sus hijos. Sin eso, VoiceOver y
TalkBack leen número, nombre y tipos como cuatro elementos que hay que recorrer
uno a uno.

**El corazón tiene etiqueta y cambia con el estado**: «Añadir a favoritos» o
«Quitar de favoritos». Un icono sin etiqueta se anuncia como «botón» y no dice
qué hace.

## El gesto que no existía para TalkBack

Quitar un favorito se hacía **solo** deslizando. Un lector de pantalla se traga
el deslizamiento, así que para quien usa TalkBack esa acción sencillamente no
existía: había que entrar en la ficha para quitarlo.

El arreglo es declararla como acción personalizada, que es donde TalkBack las
busca:

```kotlin
Modifier.semantics {
    customActions = listOf(
        CustomAccessibilityAction("Quitar de favoritos") { alQuitar(); true },
    )
}
```

iOS no necesita nada: `.swipeActions` dentro de una `List` ya se expone como
acciones de VoiceOver. Es de las pocas veces que el sistema hace el trabajo por
su cuenta.

## Lo que no se ha comprobado

Ni TalkBack ni VoiceOver se han recorrido a mano en un dispositivo real, y
tampoco se ha pasado el Accessibility Scanner. Lo de arriba es lo que se puede
verificar desde la línea de órdenes; una auditoría completa necesita un rato con
el lector de pantalla encendido y el teléfono en la mano.
