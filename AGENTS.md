# DynamicFrame — Guía para agentes y desarrolladores

**Fuente única de verdad** del proyecto. Para cambiar las reglas, edita solo este archivo.

## Qué es

**DynamicFrame** es una app Android TV (con variante **mobile**) que funciona como marco digital: slideshow de fotos y vídeos familiares con música de fondo.

- Flavors de build: `tv` y `mobile`.
- Detección de dispositivo: `LocalDeviceProfile`, `BuildConfig.IS_TV` y `UiModeManager`.
- Arranque automático al encender el TV: `BroadcastReceiver` de `BOOT_COMPLETED` (flavor TV).

**No introduzcas** librerías, capas ni patrones que no estén descritos aquí sin preguntar al usuario primero.

## Stack (no introducir otros sin consultar)

| Área | Tecnología |
|------|------------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose (+ patrones TV vía `LocalDeviceProfile` / `BuildConfig.IS_TV`) |
| Arquitectura | Clean Architecture: `domain` / `data` / `presentation` |
| DI | Hilt |
| Vídeo / audio | Media3 / ExoPlayer |
| Persistencia | DataStore (config). Room planificado cuando haya entidades locales |
| Imágenes | Coil |
| Flavors | `tv` y `mobile` |
| Fuentes de medios | Local (MediaStore) implementado. Google Photos, OneDrive, SMB/DLNA y Spotify: **placeholders** — no eliminar sin pedido explícito |

## Separación de capas (obligatorio)

- **`domain/`**: modelos, interfaces de repositorio, casos de uso (`UseCase`), motores de dominio (p. ej. `SlideshowEngine`). Sin imports del SDK de Android ni de `data/`. URIs de medios como `String`.
- **`data/`**: implementaciones (DataStore, MediaStore, Coil, ExoPlayer, receivers). No referenciada desde `presentation/`.
- **`presentation/`**: Composables y `@HiltViewModel`. Solo habla con `domain/` vía **UseCases** (y motores de dominio inyectados). No instanciar Repository, DAO, Coil ni ExoPlayer directamente.

Si una tarea requiere romper esta regla, **detenerse y preguntar** al usuario.

## Hilt

- Dependencias centralizadas en módulos `di/` existentes.
- No crear instancias manuales de Room, ExoPlayer, Retrofit, etc. en Composables o ViewModels.
- Cada ViewModel: `@HiltViewModel` + constructor inject.
- Nueva fuente de datos → nuevo módulo Hilt propio, no mezclar en módulos sin relación.

## TV vs mobile

- Lógica de negocio **una sola vez** (`domain` / `data`).
- Diferencias de UI y navegación (D-pad vs touch, tamaños) en Composables con `LocalDeviceProfile` o `device.isTv`.
- **Estado actual:** código compartido en `main`; diferencias por perfil de dispositivo.
- **Objetivo futuro:** código exclusivo de UI en source sets `tv/` o `mobile` sin duplicar lógica de negocio.
- Antes de cerrar una tarea: compilar **`assembleTvDebug`** y **`assembleMobileDebug`**.

## ExoPlayer

- `release()` en `DisposableEffect` al desmontar Composables con player local.
- Música de fondo (`MusicPlaybackService` / `MusicPlayerController` vía `MusicPlaybackRepository`) y vídeo del slideshow: instancias **independientes**.
- Errores de reproducción: `Player.Listener.onPlayerError` → reintento o saltar al siguiente medio, no parar todo el slideshow.
- No guardar referencias a Activity ni `Context` de larga vida en players.

## Motor de slideshow (`SlideshowEngine`)

- Vive en **`domain/slideshow/`**.
- Imágenes: Coil con tamaño de decodificación acotado al viewport (`rememberMaxDecodeSize`) para evitar OOM con fotos 4K+.
- Cancelar `timerJob` y `transitionJob` al navegar manualmente (sin timers en paralelo).
- Precarga de imágenes en corrutina (no bloquear el hilo principal).

## Permisos y MediaStore

- API 33+: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` y `READ_MEDIA_AUDIO` (música).
- Anteriores: `READ_EXTERNAL_STORAGE`.
- Flujo TV: navegable con D-pad (`MediaPermissions.kt`).
- Permiso denegado → banner o estado vacío claro, sin crash ni pantalla en blanco.

## DataStore

- Claves en `SlideshowPreferencesKeys` (único objeto). Sin strings literales duplicados.
- `BootReceiver` lee `SettingsBootCache` sincronizado al guardar `autoStartOnBoot`.

## Arranque automático (Boot Receiver)

- Antes de modificar autostart: verificar que `BOOT_COMPLETED` siga declarado en el manifest del flavor `tv`.
- Compatible con restricciones de inicio en background de Android 12+ (servicio en foreground si hace falta, no lanzar Activities directamente desde el receiver).

## Room (cuando se use)

- Migraciones explícitas. **No** `fallbackToDestructiveMigration()` en release.

## Compose y rendimiento en TV

- `remember` / `derivedStateOf` para evitar recomposiciones innecesarias.
- Transiciones del slideshow fluidas en hardware gama media-baja, no solo en emulador.

## Fuentes remotas (futuro)

- Misma interfaz que almacenamiento local (`MediaRepository` / `MusicRepository`).
- Sin filtrar tokens, HTTP ni detalles de API hacia `presentation/`.
- Resultado sellado: éxito / error / cargando (`LoadResult`).

## Checklist antes de cerrar una tarea

- [ ] Compilan flavors `tv` y `mobile`
- [ ] Hilt sin roturas
- [ ] Sin imports `data/` en `presentation/`
- [ ] Placeholders de fuentes futuras intactos
- [ ] Versión/changelog si el usuario lo pide en la tarea (ver `.cursor/rules/version-bump.mdc`)

## Deuda técnica conocida (no ampliar sin plan)

- Room declarado en dependencias pero sin entidades aún; al añadirlo, migraciones explícitas.
- Source sets `tv/` y `mobile/` separados: hoy el código compartido vive en `main` con `LocalDeviceProfile` / `BuildConfig.IS_TV`.
- Fuentes remotas (Google Photos, OneDrive, Spotify): placeholders sin implementar.
