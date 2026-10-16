# Pokédex · Kotlin Multiplatform

Una app de Pokédex que consume [PokeAPI](https://pokeapi.co), construida paso a
paso con **Kotlin Multiplatform** e interfaz **nativa** en cada plataforma:
Jetpack Compose en Android y SwiftUI en iOS.

Este repositorio no es una demo terminada: es el hilo de una guía. **Cada
lección deja un tag de git**, así que se puede ver el proyecto exactamente como
estaba al terminar cada paso, y el diff de lo que cambió.

```bash
git checkout l07-crear-proyecto                  # el proyecto recién creado
git diff l07-crear-proyecto l08-primer-commit    # qué añadió la lección siguiente
```

## Qué comparte y qué no

| Capa | Dónde vive |
| --- | --- |
| Modelos, red, base de datos, repositorios, ViewModels | `shared/src/commonMain` |
| Lo que solo existe en una plataforma | `shared/src/androidMain`, `shared/src/iosMain` |
| Interfaz de Android | `androidApp` (Jetpack Compose) |
| Interfaz de iOS | `iosApp` (SwiftUI, proyecto de Xcode) |

`shared` declara además un target `jvm()` que no llega a ninguna tienda: está
para que los tests corran en segundos, sin emulador ni simulador.

## Cómo ejecutarlo

Hace falta un Mac con Apple Silicon, Xcode 26.2 o superior, Android Studio con
el plugin de Kotlin Multiplatform, y un JDK 17.

```bash
# Android
./gradlew :androidApp:installDebug

# Tests, en las tres plataformas
./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:iosSimulatorArm64Test
```

Para iOS, abrir `iosApp/iosApp.xcodeproj` en Xcode y pulsar Run. Xcode llama a
Gradle solo: no hay que compilar el framework a mano. Antes de la primera
compilación, Gradle descarga la distribución de Kotlin/Native (alrededor de
1,6 GB) en `~/.konan`. Tarda.

## Cuenta de Supabase

La parte de sesión necesita un proyecto de Supabase, que es gratuito. Los pasos
están en [docs/supabase.md](docs/supabase.md).

**Android** lee las dos claves de `local.properties`, en la carpeta raíz del
proyecto. Es el fichero de ajustes de cada máquina: Android Studio lo crea solo
(guarda ahí `sdk.dir`, la ruta del SDK), está en `.gitignore` y no se comparte
con nadie. Debajo de `sdk.dir` van dos líneas, sin comillas ni espacios
alrededor del `=`:

```properties
SUPABASE_URL=https://abcdefgh.supabase.co
SUPABASE_KEY=sb_publishable_xxxxxxxx
```

El `Project URL` y la clave **publicable** salen del panel (Project Settings →
API Keys); la de servicio (`sb_secret_…`) no va nunca en una app. Gradle las
convierte en `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_KEY`
(`androidApp/build.gradle.kts`). Si faltan o están vacías, la app compila y
funciona igual: solo se queda sin la parte de cuenta.

**iOS** las lee de `iosApp/Configuration/Config.xcconfig`, a través del
`Info.plist`. Ese fichero sí es del proyecto: se deja con los dos valores
vacíos.

## Stack

Kotlin 2.4.20 · AGP 9.4.0 · Gradle 9.7.1 · targets `jvm`, `android`,
`iosArm64` e `iosSimulatorArm64`.

No hay `iosX64`: el simulador de los Mac con Intel se queda fuera porque las
librerías que usa el proyecto no publican ese target.

## Convenciones

- Identificadores y nombres de fichero en inglés; comentarios en español.
- Los comentarios explican **por qué**, no qué, y casi siempre nombran la
  alternativa que se descartó.
- Solo versiones estables, verificadas contra el repositorio Maven.

## Licencia

MIT. Ver [LICENSE](LICENSE).
