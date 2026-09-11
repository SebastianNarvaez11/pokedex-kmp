# Depurar y medir

Todo lo que hay aquí está medido en este repositorio, no copiado de un blog.

## Cuánto ocupa

`./gradlew tamanos` los imprime. Medidos el 11 sep 2026, con la app terminada:

| Artefacto | Tamaño |
|---|---|
| Framework de iOS, debug (simulador) | 81,1 MB |
| Framework de iOS, release (iosArm64) | 25,1 MB |
| App de iOS en release | 14 MB |
| APK debug | 21,8 MB |
| APK release sin firmar | 17,0 MB |

El framework de debug pesa **más de tres veces** que el de release, y eso
asusta la primera vez que se mira. No llega al usuario: lo que se publica es lo
otro. Y el framework de release tampoco se instala entero, porque es estático y
el enlazador se queda solo con lo que la app usa; la app acaba en 14 MB.

## El dSYM de Kotlin, y por qué aquí no aparece

La documentación oficial dice que el compilador produce un `.dSYM` para los
binarios de release en plataformas Apple. En este proyecto **no hay ninguno**
junto al framework:

```bash
find shared/build/bin -name "*.dSYM"
# shared/build/bin/iosSimulatorArm64/debugTest/test.kexe.dSYM
```

El motivo es que el framework es **estático** (`isStatic = true`). Un framework
estático no es un binario que se cargue aparte: se funde con el de la app al
enlazar, así que no tiene información de depuración propia que empaquetar. La
que hay acaba en el dSYM **de la app**, que genera Xcode:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme Pokedex \
  -configuration Release -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ddrel build

nm -a build/ddrel/Build/Products/Release-iphonesimulator/Pokedex.app.dSYM/Contents/Resources/DWARF/Pokedex \
  | grep 'kfun:com.sebastiannarvaez'
```

Y ahí están, con el nombre decorado que usa Kotlin/Native:

```
_kfun:com.sebastiannarvaez.pokedex#currentPlatform(){}com.sebastiannarvaez.pokedex.Platform
_kfun:com.sebastiannarvaez.pokedex.Pokedex#heartbeat(){}kotlinx.coroutines.flow.Flow<kotlin.Int>
_kfun:com.sebastiannarvaez.pokedex.Pokedex.Pokedex$heartbeat$1.$invokeCOROUTINE$0.invokeSuspend#internal
```

Ese dSYM (17 MB) es el que hay que guardar de cada versión publicada. Sin él,
un informe de fallo llega con direcciones de memoria y nada más.

### Cómo se lee un nombre decorado

```
kfun:com.sebastiannarvaez.pokedex.Pokedex#heartbeat(){}kotlinx.coroutines.flow.Flow<kotlin.Int>
└┬─┘└──────────────┬──────────────┘└───┬──┘└┬┘└┬┘└──────────────┬───────────────┘
 │                 │                   │    │  │               └ tipo de retorno
 │                 │                   │    │  └ parámetros (aquí ninguno)
 │                 │                   │    └ receptor de extensión, si lo hubiera
 │                 │                   └ nombre de la función
 │                 └ paquete y clase
 └ «kotlin function»
```

Lo que termina en `$invokeCOROUTINE$` es el cuerpo de una función `suspend`
convertido en máquina de estados. Ver ese sufijo en una traza no significa que
el fallo esté en las corrutinas: significa que la línea vive dentro de una.

## La excepción que mata el proceso

Una excepción de Kotlin que cruza a Objective-C **sin** `@Throws` no se
convierte en `NSError`: termina el proceso. El informe de fallo dice
`Uncaught Kotlin exception` y la traza apunta al puente, no al código que la
lanzó.

En esta app no hay ninguna función con `@Throws` a propósito: nada de lo que
Swift llama lanza. Los errores viajan como **estado**, dentro de `UiState`, que
además es lo que la interfaz necesita para pintarlos. Es la razón práctica de
que el núcleo devuelva `UiError` en vez de lanzar.

## Depurar Kotlin desde Xcode

El depurador de Xcode se para en el Kotlin, pero enseña los valores como si
fueran Objective-C. El plugin **xcode-kotlin** de Touchlab arregla esa parte.
Sin él, poner el punto de ruptura en el ViewModel de Kotlin y mirar las
variables desde Xcode funciona, pero se ve peor.

Para el flujo del día a día, lo práctico es al revés: la lógica se depura desde
Android Studio, donde es Kotlin de verdad, y en Xcode se depura Swift.

## Medir las corrutinas

```
-Dkotlinx.coroutines.debug
```

Añade el nombre de la corrutina al del hilo y hace legible cualquier traza. Solo
funciona en la **JVM**: en Kotlin/Native esa propiedad no existe. Es un argumento
más a favor de tener el target `jvm()`, aunque la app no se publique ahí: los
tests del núcleo corren en la JVM y ahí sí se puede.

## El recolector de basura de Kotlin/Native

No hay ningún ajuste que tocar por defecto. Si alguna vez hiciera falta mirarlo:

```
kotlin.native.binary.gcSchedulerType
```

en `gradle.properties`. Este proyecto no lo cambia, y cambiarlo sin haber medido
antes es cambiar por cambiar.
