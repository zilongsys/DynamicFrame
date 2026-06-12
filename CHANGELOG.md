# Changelog — DynamicFrame

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
