# Changelog — DynamicFrame (MEMORIA)

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
