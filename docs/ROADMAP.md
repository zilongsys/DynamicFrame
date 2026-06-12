# DynamicFrame — Roadmap y sugerencias archivadas

Documento de referencia para implementaciones futuras. Orden sugerido por impacto vs esfuerzo.

---

## Posicionamiento

| Producto | Propósito |
|----------|-----------|
| Galería del sistema | Ver y compartir fotos bajo demanda |
| **DynamicFrame** | Pantalla siempre viva: marco digital + música + ambiente |

**Gancho principal:** música de fondo sincronizada, slideshow continuo, reloj, carpetas SAF, modo TV inmersivo y autostart — cosas que la galería no ofrece.

---

## Tier 1 — Imprescindible (competir con marcos comerciales)

- [ ] **Modo marco 24/7** — Wake lock suave, atenuación nocturna, anti burn-in (movimiento sutil, rotación del reloj).
- [ ] **Entrada directa al slideshow** — Opción “al abrir → fullscreen” y “al boot → slideshow” (no solo MainActivity).
- [ ] **Perfiles por hora** — Mañana / tarde / noche: playlist, transiciones y brillo automáticos.
- [ ] **Pairing móvil → TV** — Enviar fotos al marco (QR + WebSocket o Firebase).
- [ ] **Widgets / clima / calendario** — Overlay opcional: temperatura, eventos, cumpleaños.

---

## Tier 2 — Diferenciación

- [ ] **Playlists por mood** — Álbum de fotos ↔ playlist (vacaciones, bebé, etc.).
- [ ] **Detección de presencia** — Pausa si no hay nadie (cámara o sensor).
- [ ] **Google Photos / Drive** — Sincronización en la nube.
- [ ] **Captions / historia** — Texto bajo la foto (EXIF o edición manual).
- [ ] **Transición inteligente** — Ken Burns en horizontales, slide en verticales, random ponderado.

---

## Tier 3 — Pulido premium

- [ ] **App control remoto** — Segunda app o PWA para cambiar álbum, pausar, subir fotos.
- [ ] **Estadísticas** — “Esta foto se mostró X horas este mes”.
- [ ] **Modo regalo** — Preconfiguración simple para familiares (una carpeta, una playlist, sin menús).
- [ ] **Temas visuales del marco** — Bordes, sombras, estilo museo.
- [ ] **Backup de config** — Export/import JSON de ajustes.

---

## Fuentes remotas (Fase 2 del README original)

- [ ] Google Photos API
- [ ] OneDrive (Microsoft Graph)
- [ ] SMB / DLNA / NAS

---

## Música avanzada

- [ ] Spotify Android SDK (Premium) + selección de playlist
- [ ] YouTube Music / streaming (evaluar legalidad y API)
- [ ] Temas de ambiente embebidos (THEME hoy es placeholder)
- [ ] Quitar o marcar claramente opciones no implementadas (Spotify/YouTube en UI)

---

## Deuda técnica conocida (corregir cuando toque)

- [ ] Unificar selección de álbumes (multi en Ajustes vs uno en Home)
- [ ] `MediaPermissions.pendingAction` estático — race con dos permisos
- [ ] Limpiar deps sin uso: Room, Retrofit, WorkManager
- [ ] `screenSaverMode` en config sin lógica
- [ ] `MediaSource.GOOGLE_PHOTOS`, `ONEDRIVE`, `NETWORK_SHARE` sin implementar

---

## Bugs ya corregidos (referencia)

- ViewModel destruía singletons al salir de fullscreen
- Playlist de música perdida si ExoPlayer no estaba conectado
- `loop` ignorado en el engine
- Intervalo no se re-programaba al cambiar segundos
- Shuffle reseteaba la foto actual
- Doble shuffle en use case + ExoPlayer
- Ducking con config desactualizada
- `MusicPlaybackService` exported=true
- Pausar solo paraba fotos (no música/vídeo)

---

## Mensaje de marketing (borrador)

*“Tu TV o tablet como marco digital con música — sin suscripción, tus archivos, tu control.”*

---

*Última actualización: mayo 2026*
