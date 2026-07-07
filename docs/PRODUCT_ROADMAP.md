# DynamicFrame — Roadmap de producto

Documento maestro de análisis, brechas y plan de implementación.  
**Versión de referencia del código:** v0.1.80 · **Última actualización:** junio 2026

Índice rápido: [Estado actual](#1-estado-actual-del-proyecto) · [Madurez](#2-mapa-de-madurez) · [Brechas](#3-qué-falta-por-tier) · [Orden de trabajo](#4-orden-de-implementación-recomendado) · [Nixplay vs DynamicFrame](#5-nixplay-vs-dynamicframe) · [Deuda técnica](#6-deuda-técnica) · [Checklist](#7-checklist-accionable)

---

## 1. Estado actual del proyecto

### Qué es hoy

Marco digital funcional para **Android TV** (y variante móvil) con slideshow local, música de fondo y ambientes visuales premium. Arquitectura Clean (`domain` / `data` / `presentation`), Hilt, Compose, Media3, DataStore, Coil.

**Propuesta de valor actual (honesta):**
> *Tu TV o tablet como marco digital con música — sin suscripción, tus archivos, tu control.*

### Inventario implementado

#### Núcleo de reproducción
- [x] Motor slideshow (`SlideshowEngine`): timer, shuffle foto/vídeo separado, bucle, errores consecutivos, precarga
- [x] 20+ transiciones (crossfade, Ken Burns, cube, parallax, wipe, zoom…)
- [x] Fotos + vídeos mezclados con filtro ALL / solo fotos / solo vídeos
- [x] Vídeo: ExoPlayer, reproducción completa, mute, ducking música, backdrop blur (API 31+)
- [x] Música: carpetas locales + biblioteca, multi-fuente, servicio foreground Media3, volumen independiente

#### Contenido local
- [x] MediaStore + carpetas SAF/USB
- [x] Activar/desactivar carpetas sin borrar
- [x] Álbumes y píldoras de filtro
- [x] Borrado de medios desde slideshow
- [x] Navegador de carpetas para TV sin gestor de archivos

#### Experiencia visual (diferenciador)
- [x] 4 temas de reproducción: Aurora Glass, Ambiente, Galería, Paradise
- [x] Tema Paradise: 5 capas, Ken Burns, crossfade dual, grano, clima Open-Meteo, reloj anti burn-in OLED, pill música, atribución, dots, controles auto-ocultos
- [x] Fondo letterbox dinámico: paleta dominante + blur opaco + grano
- [x] Marco dorado, zoom, borde zona segura TV, fondos letterbox variados

#### TV / sistema
- [x] Navegación D-pad completa
- [x] Arranque automático al boot (`BootReceiver`)
- [x] Permisos navegables en TV
- [x] Overscan / escala UI
- [x] Consola de depuración con export de logs

#### A medias o placeholder (no contar como «hecho»)
- [ ] `MusicSourceType.THEME / SPOTIFY / YOUTUBE` — UI en Ajustes, sin backend
- [ ] `MediaSource.GOOGLE_PHOTOS / ONEDRIVE / NETWORK_SHARE` — solo enums
- [ ] `screenSaverMode` — persistido sin lógica
- [ ] `MusicTheme` — modelo sin pistas embebidas
- [ ] Fade in/out entre pistas — parcial
- [ ] Filtros B/N, cálido, película — no implementados
- [ ] Selección de álbumes — inconsistencia Ajustes vs Home

---

## 2. Mapa de madurez

| Dimensión | % estimado | Notas |
|-----------|------------|-------|
| Reproducción local | 85% | Fuerte |
| UI / ambientes visuales | 78% | Paradise eleva mucho |
| Música local | 70% | |
| Configuración | 72% | |
| TV / D-pad | 70% | |
| Contenido remoto / nube | 10% | Crítico para mercado |
| Experiencia 24/7 | 25% | Solo KEEP_SCREEN_ON + burn-in parcial |
| Familia / compartir | 0% | |
| Onboarding / modo regalo | 15% | |
| Producto comercial (Store, tests, i18n) | 20% | |

---

## 3. Qué falta (por tier)

### Tier A — Imprescindible para producto vendible / regalable

- [ ] **Canal de contenido familiar** — pairing móvil→TV (QR + LAN/WebSocket/Firebase), o sync Google Photos/Drive
- [ ] **Modo marco 24/7** — wake lock suave, atenuación nocturna, anti burn-in global (no solo reloj)
- [ ] **Arranque «olvídate de mí»** — opción «al abrir → fullscreen»; implementar o quitar `screenSaverMode`
- [ ] **Modo regalo / kiosk** — una carpeta, una playlist, sin menús complejos
- [ ] **Perfiles por hora** — mañana/tarde/noche: álbum, transición, volumen, brillo
- [ ] **Honestidad en Ajustes** — ocultar o deshabilitar Spotify/YouTube/temas sin implementar
- [ ] **Onboarding** — 3 pasos: carpeta → música → reproducir
- [ ] **PIN / modo invitado** — proteger ajustes y borrado en salón

### Tier B — Diferenciación

- [ ] **Captions / historia** — texto bajo foto (EXIF o edición manual)
- [ ] **Playlists por mood** — álbum fotos ↔ playlist música (vacaciones, bebé…)
- [ ] **Widgets de contexto** — calendario, cumpleaños (clima parcial en Paradise)
- [ ] **Transiciones inteligentes** — Ken Burns en horizontales, slide en verticales
- [ ] **Filtros GPU** — B/N, sepia, cálido, película; contraste/saturación
- [ ] **Detección de presencia** — pausar si no hay nadie (sensor/cámara opcional)

### Tier C — Premium / escala

- [ ] **App compañera móvil** — control remoto: álbum, pausa, subir fotos, vista previa de lo que se muestra
- [ ] **Google Photos API** — sync automática de álbumes
- [ ] **OneDrive (Microsoft Graph)** — sync nube
- [ ] **SMB / DLNA / NAS** — red doméstica
- [ ] **Estadísticas** — «esta foto se mostró X veces»; favoritos; ocultar duplicados
- [ ] **Backup config** — export/import JSON de ajustes
- [ ] **Spotify SDK** (Premium) — evaluar legalidad y UX
- [ ] **Temas de ambiente embebidos** — pistas royalty-free por `MusicTheme`

### Tier D — Madurez de ingeniería

- [ ] Tests unitarios: `SlideshowEngine`, shuffle, DataStore, playlist
- [ ] Tests instrumentados: slideshow en TV emulator
- [ ] i18n — strings.xml; hoy UI mayormente español hardcodeado
- [ ] Play Store: privacy policy, crash reporting opt-in, screenshots
- [ ] Mobile flavor como **companion** (`com.dynamicframe` release, no solo debug)
- [ ] Source sets `tv/` y `mobile/` separados (UI) sin duplicar dominio
- [ ] Limpiar deps sin uso: Room, Retrofit, WorkManager (o implementar con plan)
- [ ] README y docs de usuario alineados con Paradise y temas actuales

---

## 4. Orden de implementación recomendado

Secuencia práctica si el objetivo es **valor de cliente**, no solo demo técnica:

| # | Tarea | Por qué primero |
|---|--------|-----------------|
| 1 | Honestidad UI (quitar/ocultar opciones rotas) | Confianza inmediata |
| 2 | Entrada directa slideshow + modo regalo | «Poner y olvidar» |
| 3 | Modo 24/7 (noche, burn-in global) | Uso real en salón |
| 4 | Pairing LAN móvil→TV (MVP subir fotos) | Mayor brecha vs Nixplay |
| 5 | Perfiles horarios | Sensación «inteligente» |
| 6 | Backup config JSON | Migración entre TVs |
| 7 | Google Photos **o** SMB NAS (uno primero) | Sync sin USB |
| 8 | App companion control remoto | Paridad con app Nixplay |
| 9 | Onboarding + PIN | Regalo a familiares |
| 10 | Tests motor + preparación Play Store | Escala |

---

## 5. Nixplay vs DynamicFrame

### ¿Nixplay hace lo mismo?

**En la superficie, sí:** ambos muestran un slideshow de fotos (y vídeos cortos) en una pantalla con transiciones, reloj y control remoto.

**En el modelo de producto, no:** son categorías parecidas con **distribución, hardware y ecosistema** muy distintos.

| Aspecto | Nixplay | DynamicFrame (hoy) |
|---------|---------|-------------------|
| **Hardware** | Vende marcos propios (10", 15"…) con sensor de presencia | Software: cualquier Android TV / tablet / TV box |
| **Subida de fotos** | App móvil → nube Nixplay → marco | Manual: USB, SAF, MediaStore local |
| **Pairing** | QR/WiFi; app controla el marco remotamente | No hay pairing ni vista «lo que se muestra ahora» en el móvil |
| **Google Photos** | Sync automática de álbumes (planes de pago) | Placeholder |
| **Compartir en familia** | Invitar amigos a enviar a tu marco | No |
| **Vídeo** | Clips cortos (15 s–2 min según plan) | Vídeos completos locales |
| **Música de fondo** | No es el foco del producto | **Sí** — playlist local, ducking, volumen independiente |
| **Ambiente visual** | Transiciones estándar, reloj, captions | **Paradise, Aurora, Galería**, Ken Burns, blur, clima, viñetas |
| **Presencia** | Sensor niX-Sense en hardware (despierta al entrar) | No (roadmap) |
| **IA** | Face framing, auto-rotación orientación | No |
| **Suscripción** | Basic gratis limitado; Lite/Plus de pago (almacenamiento nube) | Local, sin suscripción |
| **Privacidad** | Fotos en servidores Nixplay | Todo en dispositivo (ventaja si se comunica bien) |
| **Alexa / Google Home** | Sí | No |
| **Email / web upload** | Sí | No |
| **Modo regalo** | Precarga mensaje/fotos al regalar marco | No |

### Resumen en una frase

> **Nixplay es «marco + nube + app familiar».**  
> **DynamicFrame es «TV inteligente + slideshow premium + música»** sin infraestructura de nube ni hardware propio.

### Dónde DynamicFrame ya gana

- Vídeos largos (no limitados a 15 s–2 min)
- Música de fondo integrada con ducking
- Temas visuales de nivel ambient/screensaver (Paradise)
- Sin suscripción ni dependencia de hardware Nixplay
- Control total de archivos locales y privacidad

### Dónde Nixplay gana (y por eso «se siente» más completo al cliente)

- Subir fotos desde el móvil en segundos
- Familia remota enviando al marco
- Sync Google Photos / Dropbox
- Control remoto real («veo qué muestra el marco»)
- Sensor de presencia en el dispositivo
- Onboarding de regalo listo para abuelos

### Implicación para el roadmap

Para acercarse a la **experiencia** Nixplay sin copiar su negocio (hardware + nube de pago), DynamicFrame debe priorizar:

1. App móvil como **mando + subida** (no solo el mismo APK en teléfono)
2. **Pairing** y carpeta de recepción en el TV
3. **Google Photos** o equivalente
4. Modo 24/7 y regalo

La ventaja competitiva a mantener: **música + ambientes Paradise + local sin cuota mensual**.

---

## 6. Deuda técnica

- [ ] Unificar selección de álbumes (multi Ajustes vs Home)
- [ ] `MediaPermissions.pendingAction` estático — race con dos permisos
- [ ] `screenSaverMode` sin lógica
- [ ] Room / Retrofit / WorkManager declarados sin uso
- [ ] README desactualizado respecto a v0.1.70+

### Bugs históricos ya corregidos (referencia)

ViewModel destruía singletons; playlist música perdida; `loop` ignorado; intervalo no reprogramaba; shuffle reseteaba foto; doble shuffle; ducking stale; `MusicPlaybackService` exported; pausar solo paraba fotos.

---

## 7. Checklist accionable

Copia corta para ir tachando. Detalle en secciones anteriores.

### Tier A
- [ ] Pairing móvil → TV
- [ ] Modo 24/7 + atenuación nocturna + anti burn-in global
- [ ] Fullscreen al abrir + modo regalo
- [ ] Perfiles horarios
- [ ] Onboarding + PIN
- [ ] Ocultar opciones no implementadas en Ajustes

### Tier B
- [ ] Captions / EXIF
- [ ] Playlists mood (foto ↔ música)
- [ ] Calendario / cumpleaños overlay
- [ ] Transiciones inteligentes por orientación
- [ ] Filtros imagen

### Tier C
- [ ] App companion
- [ ] Google Photos
- [ ] OneDrive / SMB
- [ ] Estadísticas
- [ ] Backup config JSON

### Tier D
- [ ] Tests
- [ ] i18n
- [ ] Play Store readiness
- [ ] Limpiar deps / source sets tv-mobile

---

## Mensajes de marketing

**Hoy (válido):**
> *Tu TV como marco digital con música — sin suscripción, tus archivos, tu control.*

**Objetivo (cuando exista pairing + 24/7):**
> *El marco de la familia: las fotos llegan solas, la pantalla cuida de sí misma, y se ve espectacular en el salón.*

**Vs Nixplay (privacidad + sin cuota):**
> *Marco digital premium en el TV que ya tienes — tus fotos en casa, música de fondo, sin marco nuevo ni suscripción mensual.*

---

*Mantenido junto con `docs/ROADMAP.md` (índice corto) y el catálogo en `FeatureRoadmap.kt`.*
