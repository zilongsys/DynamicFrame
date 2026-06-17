# Changelog — DynamicFrame (MEMORIA)

## v0.1.32

### Añadido
- **Modo depuración**: toggle en Ajustes → Sistema; activo por defecto en builds debug.
- Consola flotante **DBG** con log de navegación, slideshow, música, vídeo, lifecycle y errores.
- Botones **Copiar** (portapapeles) y **Limpiar** para reportar fallos con contexto.
- Log también en Logcat con tag `DynamicFrame`.

## v0.1.31

### Corregido (auditoría de código)
- `SlideshowEngine.loadMedia`: actualización de estado en hilo principal tras IO.
- `SlideshowEngine.beginSession`: no marca `isPlaying` con playlist vacía.
- `MusicPlayerController`: callbacks de ExoPlayer en el hilo principal.
- `MusicPlaybackService.onDestroy`: libera player aunque falle la sesión.
- `LocalMediaRepository`: eliminado `!!` en conteo de álbumes.
- `Navigation`: limpia banner de permiso al conceder acceso.

## v0.1.30

### Corregido
- `MusicPlayerController.skipNext()`: tipo de retorno `Unit` (no el nullable de `seekToNextMediaItem`).
- `FolderBrowserDialog` de música en `HomeScreen`: parámetros `listRoots` / `listSubfolders`.

## v0.1.29

### Corregido
- Compilación KSP/Hilt: import faltante de `ImageCacheRepository` en `RepositoryModule`.

## v0.1.28

### Arquitectura (AGENTS.md)
- Vídeo del slideshow: `SlideshowVideoPlayerRepository` en domain/data; sin `ExoPlayer.Builder` en Composables.
- Imágenes: precarga vía `PreloadSlideshowImagesUseCase`; UI usa `AppAsyncImage` (Coil centralizado).
- Música: `onPlayerError` salta a la siguiente pista.
- Permisos: comprobación al inicio; banner en modo cuadro; estado compartido en navegación.
- DataStore: nombre de archivo y clave de autostart unificados en `SlideshowPreferencesKeys`.
- Corregido import de `GetLocalAlbumsUseCase` en `SettingsViewModel`.

## v0.1.27

### Arquitectura
- Limpieza de `SettingsScreen`: sin `LocalStorageBrowser` ni `folderLabel` global.
- URIs de medios como `String` en presentation (`SlideshowMediaContent`, carpetas).
- `StorageBrowserRepository` + `GetFolderDisplayNameUseCase` unifican etiquetas de carpetas.
- Permiso denegado: banner claro en álbum activo, álbumes, música y ajustes.
- `MediaPermissions` con callback `onPermissionDenied`; estado compartido en `HomeScreen`.
- Deuda técnica actualizada en `AGENTS.md` (música vía `MusicPlaybackRepository`, URIs en domain).

## v0.1.26

### Arquitectura
- Guía del proyecto en `AGENTS.md` (stack, capas, Hilt, ExoPlayer, permisos).
- `SlideshowEngine` movido a `domain/slideshow/`.
- Claves DataStore centralizadas en `SlideshowPreferencesKeys`.
- ViewModels usan solo casos de uso (sin Coil/Repository directo en presentation).
- `ImageCacheRepository` + `EvictMediaCacheUseCase` para limpiar caché al borrar medios.
- Errores de vídeo/imagen en slideshow: salta al siguiente medio sin detener la sesión.

## v0.1.25

### Corregido
- Lista al mantener OK en duración/transición: ya no se cierra sola al soltar el botón.
- Toggles del álbum activo: el recuadro violeta se marca correctamente al activar/desactivar.
- Borrar en modo cuadro sin pausar: libera imagen/vídeo antes de eliminar el archivo.
- Volumen en modo cuadro: ← → pasan al siguiente botón; ↑ ↓ ajustan volumen.

### Actualizado
- Icono aleatorio más claro (outline) en álbum activo.
- Reloj del modo cuadro centrado arriba, sin tapar controles.
- Descripciones al enfocar cada botón en el modo cuadro.

## v0.1.24

### Añadido
- Modo cuadro/teatro: al entrar, si el aleatorio está activo, empieza en foto/vídeo y pista aleatorios.
- Botón **Reiniciar** en el modo teatro: vuelve a empezar aplicando el aleatorio de imágenes y música.

### Actualizado
- Entrada al modo cuadro inicia sesión nueva (reordenación según ajustes) sin afectar al reanudar tras pausa.

## v0.1.23

### Actualizado
- Modo cuadro: icono de telón de teatro; al entrar reproduce fotos, vídeos y música (sin botón Play en el panel).
- Controles del álbum: descripción al enfocar cada botón; aleatorio con iconos grandes (foto/video/música + shuffle) sin texto.
- Fila de volumen con icono; toggles activos mantienen borde y fondo violeta.

### Corregido
- Interruptores de aleatorio, bucle y leyenda: el estado activo ya no se pierde al activar/desactivar varias veces.

## v0.1.22

### Añadido
- Modo cuadro: reproduce al entrar y se detiene al salir.
- Aleatorio independiente para fotos, videos y música.
- Icono de modo cuadro cambiado a cámara.

## v0.1.21

### Actualizado
- Álbum activo sin vista previa; controles en una fila (duración, transición, leyenda, bucle, aleatorio).
- Volumen centrado debajo de los chips; alturas fijas para evitar saltos al enfocar.

### Corregido
- Lista al mantener OK: ya no parpadea (supresión de OK corto tras diálogo).

## v0.1.20

### Actualizado
- Panel álbum activo en 2 columnas (TV y móvil): duración/volumen | transición/switches.
- Duración y transición: mantener OK abre diálogo con lista navegable (reemplaza DropdownMenu en TV).

### Corregido
- Pulsación larga en TV: detección por temporizador + repeticiones de tecla (mandos sin KeyUp).

## v0.1.19

### Actualizado
- Barra de resumen del álbum activo: iconos (fotos, videos, duración, pistas) + valor.
- Volumen TV: control único enfocable; ← → ajustan el nivel (sin saltar entre dos botones).
- Duración y transición: OK corto cicla valor; mantener OK ~0,5 s abre la lista completa.

## v0.1.18

### Corregido
- Volumen −/+ en álbum activo: foco enlazado explícitamente (← → entre botones).
- Pantalla completa: D-pad (abajo/OK) muestra controles estilo Netflix; ↓ desde pausa baja a la barra inferior.
- Reanudar ya no reinicia la canción: `play()` conserva la posición; solo carga playlist si cambió.

## v0.1.17

### Corregido
- Navegación D-pad en álbum activo: volumen −/+, sidebar ↔ contenido, controles del panel.
- La música ya no arranca sola al abrir la app; solo reproduce al pulsar Play o iniciar slideshow.

## v0.1.16

### Añadido
- Versión visible en sidebar (junto al logo), álbum activo, ajustes y móvil (como MakiX).
- Fondos letterbox configurables: 3 degradados demo + imagen personalizada.
- Utilidad `AppVersion` con formato `vX.Y.Z (code)`.

### Actualizado
- Diseño álbum activo: miniatura cuadrada, botones circulares con foco violeta.
- Imágenes en modo `Fit` sin recorte; hints temporales en pantalla completa.
- Pausa sincronizada entre dashboard, música y pantalla completa.

### Corregido
- Errores de compilación en `GlassTheme`, `SafeClickable` y `TvFocus`.
- Crash por claves duplicadas en selector de álbumes.

## v0.1.3

### Añadido
- Borrar foto o vídeo desde la app (panel y pantalla completa) con confirmación.
- Tema nostálgico rosa: UI renovada en inicio, ajustes y slideshow.

### Actualizado
- Foco D-pad en botones de ajustes, diálogos y controles del slideshow.
- Gradiente aurora rosa en modo reproducción.

## v0.1.2

### Añadido
- Explorador de carpetas integrado para TV box (sin depender de otra app para SAF).
- Atajos: Almacenamiento interno, Imágenes, DCIM, Descargas, USB/SD.

### Corregido
- «No hay app para abrir ficheros» al elegir carpeta en Android TV.

## v0.1.1

### Corregido
- Crash al abrir: bucle infinito en `dispatchKeyEvent` de MainActivity.
- Mando TV: foco D-pad mejorado (`tvClickable`, `focusGroup`, zoom sin `graphicsLayer`).
- Compilación: imports y `ProvideDeviceProfile` simplificado.

### Añadido
- Versión automática vía `app/version.properties` y tarea `bumpVersion`.
- Borde de pantalla y zoom TV en Ajustes.
- Reglas Cursor para versionado y push a GitHub (como PichiX/MakiX).
