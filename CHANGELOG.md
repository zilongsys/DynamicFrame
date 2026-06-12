# Changelog — DynamicFrame

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
- Versión visible en sidebar (`vX.Y.Z`).
- Reglas Cursor para versionado y push a GitHub (como PichiX/MakiX).
