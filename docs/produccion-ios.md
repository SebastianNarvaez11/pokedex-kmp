# Publicar la app de iOS

Todo lo de aquí está ejecutado en este repositorio. Lo único que no se puede
hacer sin Apple Developer Program (99 USD/año) está marcado.

## La versión sale del mismo sitio que en Android

`Config.xcconfig` tiene `MARKETING_VERSION` y `CURRENT_PROJECT_VERSION`, y el
proyecto los hereda con `$(inherited)`. El número coincide a mano con el del
catálogo de Gradle: son dos sistemas de compilación distintos, pero una versión
de la app es una versión, no dos.

Comprobado en el binario:

```bash
plutil -extract CFBundleShortVersionString raw Pokedex.app/Info.plist   # 1.0.0
plutil -extract CFBundleVersion raw Pokedex.app/Info.plist              # 1
```

## El manifiesto de privacidad, comprobado contra el binario

Las APIs de «motivo obligatorio» son funciones concretas, y basta con que estén
**enlazadas** para que Apple las detecte al subir. No hace falta que las llame
tu código.

```bash
nm -u shared/build/bin/iosArm64/releaseFramework/Shared.framework/Shared \
  | grep -E '_(stat|fstat|lstat|statfs|getattrlist)$'
```

Aquí devuelve `stat`, `fstat`, `lstat` y `statfs`. Ninguna es nuestra: vienen de
SQLite y del almacén de ajustes. Por eso `PrivacyInfo.xcprivacy` declara las dos
categorías correspondientes —marcas de tiempo de ficheros y espacio en disco— y
ninguna más.

La misma sonda dice lo que **no** hay: ni `NSUserDefaults` ni `systemUptime`, así
que esas categorías no se declaran. Declarar de más también da problemas: Apple
pregunta por qué.

Lo único que la app recoge es el correo de la cuenta, y solo para que funcione.
Los favoritos no se declaran porque nunca salen del dispositivo.

## El cifrado, contestado una vez y no en cada subida

```xml
<key>ITSAppUsesNonExemptEncryption</key>
<false/>
```

La app solo habla HTTPS, que es un uso exento. Sin esta clave, cada build se
queda esperando a que alguien conteste el formulario en App Store Connect.

## Crear el archivo

```bash
xcodebuild archive -project iosApp/iosApp.xcodeproj -scheme Pokedex \
  -configuration Release -destination 'generic/platform=iOS' \
  -archivePath build/Pokedex.xcarchive
```

Esto **no necesita cuenta de pago** si se añade `CODE_SIGNING_ALLOWED=NO`, y
sirve para comprobar que la compilación de release está sana. Medido aquí:

| | |
|---|---|
| App dentro del archivo (solo arm64) | 9,2 MB |
| dSYM | 17 MB, con 936 símbolos de Kotlin |

Ese dSYM es el que hay que guardar de cada versión publicada. Sin él, un informe
de fallo llega con direcciones de memoria.

## Exportar el .ipa — **requiere Apple Developer Program**

```bash
xcodebuild -exportArchive \
  -archivePath build/Pokedex.xcarchive \
  -exportOptionsPlist iosApp/Configuration/ExportOptions.plist \
  -exportPath build/ipa
```

`ExportOptions.plist` lleva `method = app-store-connect` y `teamID` vacío: cada
uno pone el suyo. `uploadSymbols` a true es lo que manda el dSYM.

Es el único paso de todo el curso que necesita la cuenta de pago. Crear el
archivo, no.

## La pantalla de lanzamiento

`UILaunchScreen` con un diccionario vacío da una pantalla **blanca**, que en
modo oscuro es un fogonazo. Con un color del catálogo que tiene su variante
oscura, el paso a la primera pantalla no se nota.
