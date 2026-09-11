# Checklist de tiendas

El curso termina aquí: con la app lista y el formulario delante. **No se sube
nada.** Publicar de verdad cuesta dinero, tiempo de revisión y decisiones que
solo puede tomar quien es dueño de la app.

Lo que sigue es la lista de lo que hay que tener preparado, con lo que ya está
hecho en este repositorio marcado.

## Antes de las dos tiendas

- [x] La app funciona en release, no solo en debug. Probada en dispositivo.
- [x] Versión en un solo sitio y la misma en los dos binarios.
- [x] Símbolos guardados: `mapping.txt` en Android, `.dSYM` en iOS.
- [x] Dos idiomas, y la app entera se lee en los dos.
- [x] La app se usa con el texto del sistema al máximo.
- [ ] **Política de privacidad hospedada en una URL pública.** Las dos tiendas
      la exigen y ninguna acepta un PDF adjunto. La página de GitHub Pages de
      este repositorio vale para eso.
- [ ] Capturas reales, no maquetas. Se sacan del emulador y del simulador.

## Google Play Console

**25 USD, pago único.** Cuenta personal o de organización.

- [ ] Crear la cuenta y verificar identidad. En cuentas personales piden
      documento y dirección, y la verificación tarda días.
- [ ] **Prueba cerrada con 12 probadores durante 14 días seguidos.** Es
      obligatorio para las cuentas **personales** creadas después del
      13 de noviembre de 2023, y es el requisito que más planes tuerce: son dos
      semanas de calendario, no de trabajo, y los 12 tienen que estar apuntados
      de forma continua. Las cuentas de organización no lo tienen.
- [ ] Subir un **AAB**, no un APK: `./gradlew :androidApp:bundleRelease`.
- [ ] Activar Play App Signing y **copiar de ahí la huella SHA-256**, que es la
      tercera que falta en `assetlinks.json`. Sin ella, los App Links dejan de
      verificar justo al publicar.
- [ ] Rellenar **Data safety**. Aquí hay que declarar lo mismo que dice el
      manifiesto de privacidad de iOS: se recoge el correo, para que la app
      funcione, no se comparte y no se usa para seguimiento.
- [ ] Clasificación de contenido, público objetivo y anuncios (no hay).
- [ ] Ficha de tienda: nombre, descripción corta y larga, icono de 512 px,
      gráfico destacado de 1024×500 y capturas de teléfono.
- [ ] Declarar que la app **permite eliminar la cuenta** desde dentro y desde
      una URL. La opción ya existe en Ajustes.

## App Store Connect

**99 USD al año.** Sin renovar, la app se retira.

- [ ] Apple Developer Program, con verificación de identidad.
- [ ] Registrar el App ID y el Bundle ID. **Son globales y únicos**: quien use
      este repositorio como plantilla tiene que cambiar el suyo.
- [ ] Subir el `.ipa` con `xcodebuild -exportArchive` o con Transporter.
- [ ] **TestFlight**, y con él la **Beta App Review**, que es una revisión de
      verdad aunque sea para pruebas.
- [ ] **Cuenta de demostración** con correo y contraseña en la ficha de
      revisión. Una app con pantalla de entrada y sin credenciales de prueba se
      rechaza sin mirar nada más, y es el motivo de rechazo más común que existe.
- [ ] Etiquetas de privacidad en la ficha, coherentes con
      `PrivacyInfo.xcprivacy`. Son dos sitios distintos con la misma
      información, y si no coinciden, lo preguntan.
- [ ] Capturas de 6,9 pulgadas. Apple ya no pide todos los tamaños, pero sí ese.
- [ ] Confirmar que la app **permite eliminar la cuenta**. Igual que en Play.

## Dos cosas que conviene saber antes de empezar

**Los identificadores son globales.** `com.sebastiannarvaez.pokedex` está
tomado. Quien siga el curso y quiera publicar tiene que cambiar el paquete, el
Bundle ID y el nombre.

**Pokémon no es de dominio público.** Esta app es un ejercicio que consume una
API pública; publicarla en una tienda con ese nombre y esas ilustraciones es
otra conversación, y no es una conversación técnica. Para el portafolio, el
repositorio y las capturas sobran.
