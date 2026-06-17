# DynamicFrame — Tracklist de peticiones

Historial de cambios pedidos por el usuario (orden más reciente primero).
Para versiones publicadas ver `CHANGELOG.md`.

---

## 2026-06-17 — Preferencias: clip, D-pad, duplicado, música aleatoria, notas, reorganización
- Estado: Completado (v0.1.38)
- Descripción: Pantalla incompleta arriba corregida; botón duplicado eliminado; notas por control añadidas (foco en TV); secciones reorganizadas; música aleatoria reubicada.

## 2026-06-17 — Regla tracklist automático
- Estado: Completado
- Descripción: Regla .cursor/rules/change-tracklist.mdc creada. TRACKLIST.md como historial de peticiones del usuario.

## 2026-06-17 — Hoja de ruta: scroll y foco D-pad en lista
- Estado: Completado (v0.1.37)
- Descripción: LazyColumn sin altura acotada impedía scroll; ítems sin focusable() en TV.

## 2026-06-17 — DataStore: instancias duplicadas crash al añadir carpeta
- Estado: Completado (v0.1.36)
- Descripción: AndroidAppDebugLogger y DataStoreSettingsRepository abrían el mismo archivo DataStore. Centralizado en Hilt (DataStoreModule).

## 2026-06-17 — Errores compilación MainActivity y Navigation
- Estado: Completado (v0.1.34)
- Descripción: Import duplicado AppDebugLogger; LaunchedEffect+onDispose reemplazado por DisposableEffect.

## 2026-06-16 — Claves duplicadas LazyColumn en Ajustes (crash al navegar)
- Estado: Completado (v0.1.33)
- Descripción: Misma URI en fotos y vídeos colisionaba como key en LazyColumn. Prefijos "photo:", "video:", "music:".

## 2026-06-16 — Modo depuración (overlay DBG)
- Estado: Completado (v0.1.32)
- Descripción: AppDebugLogger, DebugConsoleOverlay, DebugViewModel, toggle en Ajustes → Sistema.

## 2026-06-16 — Auditoría y correcciones arquitectura (domain/data/presentation)
- Estado: Completado (v0.1.31)
- Descripción: SlideshowEngine, MusicPlayerController, LocalMediaRepository, permisos, navegación.

## 2026-06-16 — Correcciones compilación (imports, skipNext, FolderBrowserDialog)
- Estado: Completado (v0.1.29–v0.1.30)
- Descripción: ImageCacheRepository en RepositoryModule; MusicPlayerController.skipNext() → Unit; FolderBrowserDialog integrado.

## 2026-06-16 — Unificación AGENTS.md y reglas del proyecto
- Estado: Completado
- Descripción: .cursorrules fusionado en AGENTS.md como fuente única de verdad.
