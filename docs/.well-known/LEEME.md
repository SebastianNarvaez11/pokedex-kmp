# Las tres huellas

El fichero `assetlinks.json` lleva **una lista**, no una huella. Aquí hay dos y
en una app publicada suelen ser tres:

1. La del **almacén de depuración** (`~/.android/debug.keystore`). Sin ella, los
   enlaces dejan de verificar en cuanto se instala una build de debug.
2. La del **almacén de release** propio, que es con la que se firma el AAB que
   se sube.
3. La de **Play App Signing**, que es la que Google usa para volver a firmar la
   app antes de entregarla. Sale de Play Console, en Configuración → Integridad
   de la aplicación, y **no se puede adivinar antes de subir nada**.

Ese tercer punto es el que rompe los App Links de casi todo el mundo: se prueba
en debug, funciona, se publica y deja de funcionar. La app que el usuario
instala está firmada con una clave que nunca estuvo en este fichero.

Se comprueban con:

```bash
adb shell pm get-app-links com.sebastiannarvaez.pokedex
```

Un estado `verified` en cada dominio es lo que hay que ver. `1024` significa que
la comprobación no llegó a hacerse.
