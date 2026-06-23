# DynamicFrame — Tracklist de peticiones

## 2026-06-22 — Rediseño fiel de los 3 temas de control (identidad propia por tema)
- Estado: Completado (v0.1.59)
- Descripción: El usuario confirmó que el indicador de tema aparece (build nuevo) pero los controles se veían iguales entre temas porque las 3 variantes compartían estilo glass + botón central idéntico. Se rehicieron fieles a los mockups: Ambiente (glifos planos blancos, play con halo morado, sin glass), Galería (riel bronce/oro a la derecha + placa de pergamino con serif), Aurora Glass (HUD glass). Nuevos botones por tema (`AmbientFlatIcon`, `AmbientPlayButton`, `GalleryRailButton`) con foco D-pad y hints. Se regeneraron las 3 imágenes de referencia.

## 2026-06-22 — Diagnóstico "los temas no se aplican" + indicador de tema activo
- Estado: Completado (v0.1.58) — pendiente de confirmación del usuario tras rebuild limpio
- Descripción: El usuario reporta que los temas no cargan nunca (ni controles ni encuadre cambian). Se auditó la cadena completa (Ajustes→DataStore→observeConfig→SlideshowEngine→ViewModel→SlideshowScreen/dispatcher) y está correcta; todos los componentes resuelven. Como no hay JDK en esta máquina (build lo hace Android Studio del usuario), se añade un indicador "Tema: …" de 2,5 s al entrar a pantalla completa que confirma si el APK incluye los cambios. Si no aparece tras Clean Project + reinstalar, el problema es de build/caché local.

## 2026-06-22 — Temas aplican al contenido, detener todo al salir, secciones colapsables con color
- Estado: Completado (v0.1.57)
- Descripción: (1) Los temas de reproducción ahora transforman también el contenido, no solo el overlay: Galería envuelve la foto/vídeo en un paspartú de museo (`GalleryMatFrame`), Ambiente va a sangre sin marco y Aurora Glass respeta el marco dorado opcional; así el cambio de tema se ve de inmediato (antes "se veía igual" porque solo cambiaban los controles ocultos). (2) Al salir de pantalla completa se detiene TODO (motor + vídeo + música) con el nuevo `stopSlideshow()` en el `onDispose`. (3) Ajustes: cada sección se envuelve en `SettingsSectionCard` con fondo del color de acento aplicado a todas sus opciones y cabecera pulsable para colapsar/expandir.

## 2026-06-22 — Pantalla de reproducción: 3 temas de interfaz seleccionables (Aurora Glass por defecto)
- Estado: Completado (v0.1.56)
- Descripción: Tras analizar la pantalla de reproducción y proponer 3 estilos con mockups, se implementan los 3 como temas elegibles (enum `PlaybackTheme`, persistido en DataStore): Aurora Glass (HUD glass, por defecto), Ambiente (minimalista con scrim) y Galería (riel lateral + placa de datos). Selector en Ajustes → Visual. Nuevo `PlaybackControlThemes.kt` con dispatcher `PlaybackControlsOverlay` y modelo `PlaybackControlsCallbacks`. Además se arreglan bugs comunes: auto-ocultado de controles (TV no ocultaba si el foco no estaba en play/pausa; móvil ocultaba a mitad de gesto), estado vacío engañoso y reentrancia del formateador del reloj.

## 2026-06-22 — Auditoría núcleo: 5 bugs corregidos (v0.1.55)
- Estado: Completado (v0.1.55)
- Descripción: (1) `removeItem()` ya no re-baraja la playlist al borrar un ítem. (2) `prepare()` en ExoPlayer elimina `stop()+clearMediaItems()` para evitar flash negro. (3) `isDucked` stale: `resumePlayback()` limpia el estado de duck antes de restaurar volumen. (4) `musicCoordinator.disconnect()` añadido a `onCleared()`. (5) KEN_BURNS: `initialScale` corregido de 1.0f a 0.92f para que la animación sea visible.

## 2026-06-17 — General: scrollbar lateral, foco al entrar, icono shuffle, volumen vídeo enlazado, colores por sección
- Estado: Completado (v0.1.54)
- Descripción: (1) Scrollbar en la barra lateral principal (`Modifier.verticalScrollbar`). (2) Al pulsar flecha derecha desde el menú el foco entra al primer elemento focable de la sección (`focusProperties { right = contentFocus }` + `focusRequester` sobre el `focusGroup` del contenido). (3) Nuevo icono de aleatorio unificado a partir del SVG aportado (`ic_shuffle_dynamic` / `ShuffleIcon`), reemplaza todos los random de Ajustes y Dashboard. (4) Control de volumen de vídeo en Preferencias (sección Videos) enlazado con el de reproducción vía `config.mediaVolume`; volumen de música también persistido en `config.musicVolume` para enlazar todas sus instancias. (5) Colores de acento por sección en Preferencias para identificar visualmente dónde estás.

## 2026-06-18 — Video sin negro, volumen separado, scroll roadmap sin salto
- Estado: Completado (v0.1.53)
- Descripción: Vídeo fuera de AnimatedContent (no se recrea entre vídeos), shutter transparente + fade-in de primer frame → sin flash negro ni distorsión. Nuevo control de volumen de audio del vídeo en el panel de reproducción, independiente del volumen de música. Scroll de la hoja de ruta reemplazado: BringIntoViewSpec eliminado, ahora usa animateScrollToItem controlado por tracking de foco en cada ítem.

## PENDIENTE — Fondo de pantalla personalizado (wallpaper)
- Estado: Pendiente
- Descripción: El usuario pide poder elegir un fondo de pantalla propio para cubrir las áreas que no llenan las fotos/vídeos. La infraestructura ya existe (PlaybackBackgroundType.CUSTOM_IMAGE + playbackBackgroundImageUri en SlideshowConfig, selector en SettingsScreen). Verificar que el selector en Ajustes → Visual en reproducción funcione correctamente y sea visible para el usuario.

Historial de cambios pedidos por el usuario (orden más reciente primero).
Para versiones publicadas ver `CHANGELOG.md`.

---

## 2026-06-18 — Vídeo: pausa al terminar (causa real) + imagen de fondo persistente tras vídeo
- Estado: Completado (v0.1.52)
- Descripción: El fin de vídeo se detectaba con un listener que se re-registraba en cada recomposición (lambdas cambiantes), perdiendo a veces STATE_ENDED; sin temporizador de respaldo en modo "vídeo completo" el vídeo quedaba congelado. Ahora un único listener estable (rememberUpdatedState) maneja fin/errores/tamaño de forma fiable. Además, la capa underlay (foto previa) ya no se dibuja detrás del vídeo: solo bajo imágenes y se limpia al pasar a vídeo, así el vídeo se ve sobre el fondo de reproducción.

---

## 2026-06-18 — Vídeo: al terminar se pausaba en vez de avanzar/repetir; verificar aleatorios
- Estado: Completado (v0.1.51)
- Descripción: Añadido playToken en SlideshowState que se incrementa en cada navegación; el reproductor re-prepara al cambiar URI o playToken, reiniciando de forma determinista el mismo vídeo (bucle con un único vídeo). Multi-elemento ya avanzaba. Verificados los aleatorios: fotos/vídeos se barajan por tipo en buildPlaylist (re-baraja por sesión, arranque aleatorio en beginSession); música pre-barajada + REPEAT_MODE_ALL. Duración de vídeo: completo si está marcado, si no usa el intervalo de fotos.

---

## 2026-06-18 — Vídeo: pantalla en negro al pausar/reanudar (solo vídeos)
- Estado: Completado (v0.1.50)
- Descripción: La v0.1.49 usó TextureView (provoca negro al reanudar). Se revierte a SurfaceView (recomendado para TV) y la distorsión inicial se corrige forzando re-layout en onVideoSizeChanged/onRenderedFirstFrame + keepContentOnPlayerReset. Además stopIfCurrent(uri) al desmontar para no cortar el vídeo del siguiente slide en transiciones vídeo→vídeo.

---

## 2026-06-18 — Aleatorio de música real y vídeo sin distorsión
- Estado: Completado (v0.1.49)
- Descripción: El aleatorio de música ahora baraja la lista antes de reproducir (cada sesión empieza por una canción distinta), en vez de depender del shuffle de ExoPlayer que solo afecta al "siguiente"; comparación de lista obsoleta por conjunto de ids. El vídeo pasa a usar TextureView (antes SurfaceView), eliminando la distorsión inicial durante las transiciones: se muestra completo en su resolución (RESIZE_MODE_FIT, sin recortar) y correcto desde el primer frame, igual que las fotos.

---

## 2026-06-18 — Preferencias: carpetas de fotos aparecían en vídeos; mover Música tras Videos
- Estado: Completado (v0.1.48)
- Descripción: Corregido que la sección de vídeos mostrara carpetas de fotos (fallback a MEDIA_FOLDERS legacy en cada guardado): ahora solo se migra si la clave específica nunca se guardó. Sección "Música de fondo" reubicada justo después de "Videos".

---

## PENDIENTE — Hoja de ruta: persiste un efecto de "salto" al desplazar
- Estado: Pendiente
- Descripción: Tras los arreglos de scroll (v0.1.44–v0.1.47) el usuario sigue percibiendo un efecto de salto al desplazarse por el listado de la hoja de ruta. Queda por investigar/resolver. Próximas hipótesis a probar: control de scroll totalmente manual por índice enfocado (con buffer para no romper la navegación con D-pad), o revisar bring-into-view/animación restante.

---

## 2026-06-18 — Hoja de ruta: scroll ágil (90 ms) y anillo de foco visible sobre el chip activo
- Estado: Completado (v0.1.47)
- Descripción: Reducida la animación de auto-scroll a 90 ms (sin retardo al navegar rápido). Los chips de filtro muestran un anillo blanco al estar enfocados, de modo que se ve el foco incluso sobre el chip activo en violeta.

## 2026-06-18 — Hoja de ruta: scroll suave definitivo (fin del brinco) con BringIntoViewSpec propio
- Estado: Completado (v0.1.46)
- Descripción: El brinco venía de la animación spring del auto-scroll por foco. Se sustituye por un BringIntoViewSpec con desplazamiento mínimo (filas visibles no se mueven, D-pad sigue funcionando) y animación tween suave → scroll natural.

## 2026-06-18 — Hoja de ruta: scroll natural sin salto por línea y primera fila al entrar (robusto)
- Estado: Completado (v0.1.45)
- Descripción: Eliminado el salto por línea moviendo el espaciado a margen fuera del área enfocable (el bring-into-view ya no sobre-desplaza). La primera fila se marca siempre al entrar en la lista mediante onFocusChanged (seguro, sin la API experimental que crasheaba).

## 2026-06-18 — Hoja de ruta: crash al navegar, salto al desplazar y foco izquierdo a la sección correcta
- Estado: Completado (v0.1.44)
- Descripción: Corregido el crash que cerraba la app (eliminado focusProperties enter con FocusRequester no adjunto). Eliminado el salto al subir/bajar (allItems memoizado; scrollToItem(0) solo al cambiar filtro). Al salir con flecha izquierda el foco vuelve al item del menú de la sección actual (focusProperties exit).

## 2026-06-17 — Hoja de ruta: marcar 1ra fila al entrar, quitar brinco al desplazar, chips activo=violeta/enfocado=gris
- Estado: Completado (v0.1.43)
- Descripción: Al entrar en la lista se marca la primera fila (focusProperties enter + foco inicial). Eliminado el "brinco" al subir/bajar fijando la altura de fila (peso de fuente constante + maxLines). Chips de filtro con comportamiento moderno: activo = violeta completo, enfocado (no activo) = gris, reposo = contorno.

## 2026-06-17 — Hoja de ruta: foco morado completo, caer en primera fila, volver al menú al salir; botones morado al enfocar
- Estado: Completado (v0.1.42)
- Descripción: Filas de la hoja de ruta enfocadas con relleno morado completo y texto blanco. Al entrar en la lista el foco cae en la primera fila. Al salir por la izquierda el foco vuelve al ítem seleccionado del menú lateral (FocusRequester compartido + focusProperties left). Todos los botones (NostalgiaActionButton, DeviceActionButton, TvPickerChip, TvStepperChip y menú lateral) se rellenan de morado completo con contenido blanco al enfocar. Nuevo parámetro focusScale en safeClickable/tvClickable.

## 2026-06-17 — Auditoría completa: arreglar todo paso a paso
- Estado: Completado (v0.1.41)
- Descripción: Tras analizar todo el proyecto, corregidos errores críticos y lógicos: carpetas desactivadas ahora se excluyen de fotos/vídeos/música; multi-fuente de música real; el vídeo se detiene al cambiar de slide y la pausa ya no lo reinicia; música no se atenúa con vídeo mudo; el botón de música solo controla la música; contador anti-bucle si todos los medios fallan; autostart TV más robusto (try/catch, consume AUTO_STARTED, abre slideshow); banner de permisos accionable; límites de decodificación Coil (miniaturas y fondo); race condition de DataStore serializada; textos del roadmap más grandes en TV; fuentes de música no implementadas atenuadas/deshabilitadas; mensaje de borrado claro en Android 10+.

## 2026-06-17 — Preferencias: clip, D-pad, duplicado, música aleatoria, notas, reorganización
- Estado: Completado (v0.1.38)
- Descripción: Pantalla incompleta arriba corregida; botón duplicado eliminado; notas por control añadidas (foco en TV); secciones reorganizadas; música aleatoria reubicada.

## 2026-06-17 — Foco roadmap profesional, toggle carpetas, papelera D-pad, shuffle icons, multi-fuente música, fix +/- volumen
- Estado: Completado (v0.1.40)
- Descripción: Rediseño foco hoja de ruta (barra lateral animada, sin salto). Toggle activar/desactivar carpetas. Botón papelera independiente y navegable. Iconos shuffle. Multi-select fuentes de música. Fix definitivo botones +/- de volumen con focusGroup.

## 2026-06-17 — Hoja de ruta scroll, filtro, foco; Preferencias FolderChip, ruta, reorg
- Estado: Completado (v0.1.39)
- Descripción: Scrollbar + filtro por estado en hoja de ruta; restauración de foco estilo Netflix; FolderChip con ruta completa y focusable en TV; Preferencias reorganizadas por tipo de contenido; versión al final.

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
