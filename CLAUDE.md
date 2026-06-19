# DynamicFrame — Guía para Claude Code

La **fuente única de verdad** del proyecto es `AGENTS.md`. Lee ese archivo antes de escribir o modificar código.

## Roles en este proyecto

| Agente | Herramienta | Dispara |
|--------|-------------|---------|
| **Cursor AI** | `.cursor/rules/*.mdc` | Edición en el editor Cursor |
| **Claude Code** | Este archivo + `AGENTS.md` | CLI claude / Claude Code desktop |

Ambos agentes pueden trabajar en el mismo repositorio. Las reglas de coordinación están abajo.

## Reglas que aplican aquí (derivadas de `.cursor/rules/`)

### Versión
- Fuente: `app/version.properties`. NO editar `versionName` a mano en `build.gradle.kts`.
- Subir versión solo al entregar cambios de código: `./gradlew :app:bumpVersion` o editar `version.properties` a mano.
- PATCH 0–100; al llegar a 100 → MIDDLE+1, PATCH=0.
- Documentar en `CHANGELOG.md` con `## vX.Y.Z` usando **Añadido** / **Actualizado** / **Corregido** / **Eliminado**.

### Git (coordinación con Cursor — CRÍTICO)
1. Antes de hacer commit: ejecutar `git pull --rebase origin main` para integrar cambios de Cursor.
2. Hacer commit solo al **terminar** la tarea (código compilable, sin secretos).
3. No incluir: `.env`, `local.properties`, `*.jks`, keystores.
4. Mensaje de commit en español o inglés; mencionar `vX.Y.Z` al actualizar versión.
5. Comandos:
```powershell
git pull --rebase origin main
git add -A
git -c user.name="onlyeyes" -c user.email="onlyeyes@users.noreply.github.com" commit -m "mensaje (vX.Y.Z)"
git push origin HEAD
```
6. Si `git push` falla por auth: indicar `gh auth login` y reintentar una vez.
7. **No commit** si: solo preguntas, revisión sin editar, o el usuario dice "sin commit"/"no subas".

### Tracklist
En cada respuesta que incluya un cambio de código:
1. Abrir `TRACKLIST.md`.
2. Añadir al inicio (orden descendente):
```
## YYYY-MM-DD — <nombre corto del cambio>
- Estado: Pendiente | Completado | Parcial
- Descripción: <una línea>
```
3. Actualizar estado de entradas anteriores si la tarea actual las resuelve.

### Pantalla Preferencias / Ajustes
Respetar el orden de secciones de `.cursor/rules/settings-screen.mdc`:
1. Contenido → 2. Slideshow → 3. Visual en reproducción → 4. Reloj y fecha →
5. Música de fondo → 6. Videos → 7. Pantalla TV (solo TV) → 8. Sistema

Cada control debe tener `note = "…"`. Ver el `.mdc` para detalles de D-pad y Compose.

## Checklist antes de cerrar una tarea
- [ ] Compilan flavors `tv` y `mobile` (`assembleTvDebug` + `assembleMobileDebug`)
- [ ] Hilt sin roturas
- [ ] Sin imports `data/` en `presentation/`
- [ ] Placeholders de fuentes futuras intactos
- [ ] `TRACKLIST.md` actualizado
- [ ] `CHANGELOG.md` + `version.properties` si hay cambio de versión
- [ ] `git pull --rebase` antes de commit (coordinación con Cursor)

## Cuándo NO commitear
- Solo preguntas o análisis sin cambios de archivo.
- El usuario dice explícitamente "sin commit", "no subas", "solo muéstrame".
- Hay un merge conflict no resuelto tras el `git pull --rebase`.
