# Changelog — DynamicFrame (MEMORIA)

## v0.1.90

### Corregido
- **Borrar en slideshow**: captura la foto/vídeo actual al pulsar borrar; pausa slideshow, vídeo y música; diálogo con miniatura; borrado MediaStore/SAF con consentimiento del sistema si hace falta; reanuda al terminar.

## v0.1.89

### Corregido
- **Fondo sin imagen / cuadro blanco**: requests Coil de precarga alineados con la UI (`buildSharpImageRequest`); capa de retención con `RetainedCompleteImageSlide` (no parpadea al cambiar URI); fondo+foto solo juntos en `Success`.

## v0.1.88

### Corregido
- **Cuadro blanco al cambiar foto**: la capa de retención ya no recarga la nueva imagen durante la transición; `AppImage` solo pinta con Coil en `Success`; crossfade global de Coil desactivado.

## v0.1.87

### Corregido
- **Flash negro sin transición**: capa `HeldSlide` mantiene la foto anterior visible hasta que la nueva está pintada; `NONE` usa swap de 1 ms en lugar de corte seco.
- **Flash negro leve con transición**: el slide entrante ya no reserva un `Box` vacío; la capa de retención rellena los huecos del fundido.

## v0.1.86

### Añadido
- **Ajustes → Slideshow**: vista previa animada de la transición seleccionada y descripción del efecto en la nota del control.

## v0.1.85

### Corregido
- **«Solo fondo» al cambiar de foto**: el slide dinámico no se pinta hasta que Coil confirma la imagen nítida (`AppImageWhenReady`); fondo y foto aparecen juntos dentro de la transición `AnimatedContent`.
- **Transición ignorada**: ya no se fuerza `TransitionType.NONE` cuando el siguiente slide es un vídeo (foto→foto respeta el estilo configurado).

## v0.1.84

### Corregido
- **Fondo adelantado respecto a la foto**: fondo y foto usan la misma decodificación (960×540, caché del prefetch); bloque unificado `DynamicImageSlide`.
- **Parpadeo / «refresh» de la imagen**: `AppAsyncImage` memoriza el request Coil; sin underlay en modo dinámico; crossfade Coil a 0 (solo anima `AnimatedContent`); paleta fijada por `item.id`.

## v0.1.83

### Corregido
- **Fondo de la siguiente foto visible antes que la foto**: el letterbox dinámico va dentro del mismo `AnimatedContent` que la imagen; cambian juntos en la transición.
- **Reproducción antes de estar listo**: «listo» incluye paleta + imagen nítida + blur; música y slideshow no arrancan hasta `awaitFirstSlideReady` (overlay «Preparando fondo…»).

## v0.1.82

### Corregido
- **Fondo dinámico empeorado (regresión v0.1.81)**: eliminado el desacople `displayedSlide` que ocultaba la UI y pausaba el motor entre fotos. El motor ahora **espera** a que el backdrop esté listo **antes** de cambiar de slide (foto anterior visible, música sin cortes).
- **Precarga inútil del blur**: `AppBlurFillImage` usaba petición Coil distinta a la precarga (sin tamaño 960×540); ahora comparten caché.
- **Precarga bloqueada por blur**: «listo» = paleta + imagen nítida; blur en segundo plano.
- **Música**: `setPlaybackAllowed(false)` ya no pausa ExoPlayer; `refreshPlaylist` solo si la lista cambió.

### Añadido
- `SlideshowEngine.setBeforeNavigate`: gancho suspend antes de cada cambio de diapositiva.

## v0.1.81

### Corregido
- **Música que se cortaba entre slides**: eliminado `haltAllPlayback()` en cada cambio de diapositiva; `refreshPlaylist` conserva `isPlaying` al actualizar la lista de música.
- **Imagen visible sin degradado listo**: el viewport usa `displayedSlide` (desacoplado del motor) y solo muestra la foto cuando el fondo dinámico está precargado.

### Añadido
- **`DynamicBackdropPrefetcher`**: ventana rolling de 6 slides (actual + 5 siguientes) con paleta, imagen nítida y blur; recarga al avanzar.

## v0.1.80

### Corregido
- **Paradise — fondo negro al abrir**: `SlideshowMediaViewport` ya recibe `skipLetterboxBackground=true` desde Paradise. Sin este flag pintaba `PlaybackLetterboxBackground(BLACK)` encima del blur de fondo, tapando las áreas de letterbox con negro en vez de mostrar el blur. La capa blur (`ParadiseBlurBackdrop`) ya rellena esas zonas; la capa viewport no debe añadir fondo propio.
- **Paradise — D-pad muerto al aparecer controles**: al hacer OK el `LaunchedEffect` de foco llamaba `pauseFocus.requestFocusWhenReady()`, pero `pauseFocus` solo está anclado en `PlaybackControlsOverlay` (no se muestra en Paradise). Añadido `paradiseFocus` (FocusRequester) anclado al primer botón de `ParadiseScreensaverControls`. El `LaunchedEffect` ahora usa `paradiseFocus` cuando `isParadise && controlsShown`.
- **Paradise — carrera de reproductor de vídeo en transición**: el `AnimatedContent` externo compone dos instancias del viewport simultáneamente durante el crossfade. Ambas intentaban usar el `SlideshowVideoPlayerRepository` singleton. Ahora con `externalCrossfade=true` solo la instancia activa (`isPlaying=true`) monta `SlideshowVideoPlayer`; la instancia saliente (`isPlaying=false`) no lo renderiza y no dispara `stopIfCurrent` al desmontarse.

### Añadido
- `ParadiseScreensaverControls`: nuevo param `firstButtonFocus: FocusRequester?` y `focusGroup()` en la Row del pill para habilitar navegación D-pad entre los botones.
- **Fondo letterbox «Dinámico»** (`PlaybackBackgroundType.DYNAMIC`): degradado de colores predominantes (Palette) + misma foto/vídeo en crop con blur opaco (~78 %), igual que Paradise. Selector en Ajustes → Visual en reproducción.

## v0.1.79

### Corregido
- **Paradise — verificación Bloque 11:** viñetas no interceptan foco/toques; texto overlay solo `White.copy`; backdrop vídeo liberado junto al player principal; gradientes capa 3 sin hex en texto.

## v0.1.78

### Actualizado
- **Paradise — Bloque 10:** controles ocultos por defecto; OK/Center muestra pill inferior (pause, skip, volumen) 4 s; `controlsVisible` en ViewModel.

## v0.1.77

### Actualizado
- **Paradise — Bloque 9: crossfade** capa sharp 1200 ms y blur 800 ms vía `AnimatedContent` por índice; solo fade; Ken Burns reinicia por foto.

## v0.1.76

### Añadido
- **Paradise — Bloque 8: Ken Burns** en capa sharp (solo fotos): zoom 1.0→1.04 en 30 s y pan aleatorio ±1 %; blur estático.

## v0.1.75

### Actualizado
- **Paradise — Bloque 7:** dots de paginación (8 visibles, ancho animado 3→16 dp) y hint TV «▼  Settings» (10 s + fade 600 ms; OK abre controles).

## v0.1.74

### Actualizado
- **Paradise — Bloque 6: pill de música** inferior centro (`BottomCenter`, 52 dp); fade 400 ms; visible solo con pista en reproducción.

## v0.1.73

### Actualizado
- **Paradise — Bloque 5: atribución de foto** inferior derecha (álbum/carpeta + «Photo by …»), sin fondo; `AnimatedVisibility` solo con `albumName` válido.

## v0.1.72

### Añadido
- **Paradise — Bloque 4: clima** en esquina superior derecha; `WeatherUseCase` + Open-Meteo (sin API key), geolocalización por IP, caché DataStore 30 min; oculto si falla o no hay red.

## v0.1.71

### Actualizado
- **Paradise — Bloque 3: reloj** en esquina inferior izquierda (`h:mm a` + `EEEE, MMMM d`), tipografía sin fondo/sombra sobre viñetas, offset anti burn-in OLED cada 60 s.

## v0.1.70

### Corregido
- **Paradise no activaba el fondo blur** si solo se cambiaba «Tema de la interfaz»: ahora **Paradise** es la 4.ª opción en reproducción y ambos selectores se sincronizan (`isParadiseActive()`).
- **Barras negras visibles**: blur a opacidad plena (1.0); capa crop de respaldo bajo el blur en fotos.

## v0.1.69

### Corregido
- **Build:** eliminada dependencia inexistente `io.coil-kt:coil-transformations:2.6.0` (Coil 2.x ya no la publica). `BlurTransformation` local en `ui/coil/`.

## v0.1.68

### Añadido
- **Paradise — Bloque 2: blurred background fill** (solo tema Paradise):
  - **Fotos:** `AppBlurFillImage` con Coil `BlurTransformation` (radius 25, sampling 4), crop, alpha 0.65, saturación 1.3.
  - **Vídeos API 31+:** segundo `PlayerView` mudo (crop/zoom) con `RenderEffect` blur 40px; reproductor principal sigue en Fit (capa 2).
  - **Vídeos API &lt; 31:** fotograma vía `MediaMetadataRetriever` + mismo blur que fotos.
- Dependencia `coil-transformations` no existe en Coil 2.x; `BlurTransformation` copiada en `data/local/coil/`.

## v0.1.67

### Añadido
- **Tema global Paradise** (`AppTheme.PARADISE`): selector en Ajustes → Visual → «Tema de la aplicación»; persistido en DataStore (`app_theme`). `LocalAppTheme` en toda la app vía `AppRoot`.
- **Visualizador Paradise** (solo con Paradise activo): 5 capas — (1) blur crop de imagen o poster estático de vídeo, (2) medio sharp Fit sin letterbox, (3) tres viñetas degradadas, (4) overlays de info (reloj, chip música, atribución, puntos, hint), (5) controles auto-ocultos (HUD Aurora provisional).

### Notas
- Aurora Glass / Ambiente / Galería sin cambios cuando el tema de app es Memoria.
- Weather: pendiente en Paradise. Blur vídeo: poster estático (Coil fotograma 0). API &lt; 31: fallback sin blur nativo.

## v0.1.66

### Actualizado
- **Volúmenes Aurora Glass (TV)**: con botones ± activos, la barra de nivel es solo visual (no recibe foco ni ← →).
- **Hints de ±** específicos: «Bajar/Subir volumen de la música» y «Bajar/Subir volumen del vídeo».

## v0.1.65

### Actualizado
- **Volúmenes Aurora Glass más legibles**: pista 92 dp, icono 16 dp, bolita 12 dp; botones **±** enfocables a los lados para navegar con D-pad sin depender solo de ← →.
- **Franja de controles más baja**: menos padding en el HUD, píldoras de álbum compactas (54 dp), barra de progreso fina y botones de transporte 42 dp en la fila inferior.

## v0.1.64

### Actualizado
- **Aurora Glass — volúmenes en la fila de transporte**: música y vídeo con track corto (68 dp) entre anterior/siguiente y los botones de música, en la **misma línea** que replay/prev/next.
- **Bolita de volumen visible**: thumb cyan posicionado según el nivel (incluido volumen 0), sobre pista fina con tramo activo degradado.

## v0.1.63

### Corregido
- **Volumen de vídeo siempre visible** en Aurora Glass (cuando «Silenciar vídeo» está desactivado): ya no depende de que el slide actual sea un vídeo.
- **Volúmenes apilados** (música arriba, vídeo abajo), compactos, con icono **MusicNote** / **Movie** según el medio.

### Actualizado
- **Miniaturas de carpetas de vídeo**: la píldora usa el **1.er vídeo** de la carpeta; Coil registra `VideoFrameDecoder` y `AppAsyncImage` pide el fotograma 0 para URIs de vídeo.

## v0.1.62

### Actualizado
- **Aurora Glass — layout y miniaturas fieles al mockup**:
  - Botón **play/pausa** centrado **justo encima** del HUD inferior (ya no flota en el centro de la pantalla).
  - **Volúmenes de música y vídeo** en fila **paralela compacta** (`compact = true`), debajo de los botones de transporte.
  - Píldoras de álbum con **miniatura real**: 1.ª foto del álbum/carpeta vía `AlbumPillOption.thumbnailUri` + `AppAsyncImage` (Coil); respaldo con `coverUri` del MediaStore.
  - Paleta cyan eléctrico `#00E5FF` sobre cristal oscuro `#101828` / `#121E30` (sin morado genérico).

## v0.1.61

### Actualizado
- **Aurora Glass rediseñado para coincidir con el mockup** (ya no reutiliza la UI genérica de Ajustes):
  - Nuevo `AuroraGlassComponents.kt`: paleta cyan eléctrico (`#00E5FF`) sobre cristal oscuro, HUD con borde degradado, botones circulares superiores, play central con **halo cyan** (no morado MemoriaPurple).
  - **Eliminados los steppers 40%/100%** (`TvVolumeStepper`) del HUD de reproducción; sustituidos por **sliders finos** (`AuroraGlassVolumeSlider`) en una sola fila con transporte.
  - Píldoras de álbum, barra de progreso cyan, chip de música y reloj con estilo Aurora coherente.
  - Panel inferior compacto: transporte + volumen + música en **una fila** (como el mockup), no dos cajas apiladas.

## v0.1.60

### Corregido
- **Los temas Ambiente y Galería no se veían hasta pulsar OK/tocar**: toda la UI temática estaba dentro de `AnimatedVisibility(showControls)`, que arranca en `false`. Solo se veían la foto y el badge "Tema: …". Ahora **Ambiente** (barra inferior minimalista + halo morado) y **Galería** (riel bronce a la derecha + placa de pergamino) se muestran **siempre**, como en los mockups. Aurora Glass sigue ocultando controles hasta interactuar.
- En TV, con Ambiente/Galería el foco entra directamente en los controles del tema (no hace falta pulsar OK primero).
- El badge de diagnóstico incluye la versión (`Tema: Galería · v0.1.60`) para confirmar que el APK está actualizado.

## v0.1.59

### Actualizado
- **Rediseño fiel de los 3 temas de control** para que cada uno tenga identidad visual propia (antes los tres compartían el estilo "glass" y un botón central idéntico, por lo que parecían iguales):
  - **Ambiente**: minimalismo real — glifos planos blancos sin cajas/glass, botón central de play/pausa con **halo morado radial**, acciones superiores discretas a baja opacidad y barra de progreso fina y corta sobre un scrim degradado.
  - **Galería**: estilo museo — **riel vertical de botones circulares bronce con borde dorado** acoplado a la derecha, y **placa de pergamino** (no glass) centrada abajo con fecha en tipografía serif, álbum en cursiva y posición.
  - **Aurora Glass**: HUD glassmorphism inferior (sin cambios).
- Nuevos botones específicos por tema (`AmbientFlatIcon`, `AmbientPlayButton`, `GalleryRailButton`) con foco D-pad, escala al enfocar y hints, en lugar de reutilizar el botón glass común.

## v0.1.58

### Añadido
- **Indicador del tema activo al entrar a pantalla completa**: muestra "Tema: Aurora Glass / Ambiente / Galería" durante 2,5 s. Sirve también de diagnóstico: si no aparece, el APK en ejecución no incluye los cambios de temas (build en caché/desactualizado).

### Notas (diagnóstico "los temas no se aplican")
- Se verificó la cadena completa y es correcta: Ajustes (`updateConfig` → `UpdateSlideshowConfigUseCase` → `saveConfig`) escribe `PLAYBACK_THEME` en DataStore; `observeConfig()` emite el nuevo `SlideshowConfig`; `SlideshowEngine._config` lo recibe; `SlideshowViewModel.slideshowConfig` lo expone; `SlideshowScreen` conmuta el encuadre del contenido (`GalleryMatFrame`/a sangre/`PictureFrame`) y `PlaybackControlsOverlay` despacha al tema correcto. Todos los componentes (`MediaCircleButton`, `CenterPlayPauseButton`, `GlassSurface`, etc.) resuelven.
- Conclusión: si tras reconstruir limpio el tema sigue sin verse, el problema es de build/caché en el equipo, no del código. Recomendado: *Build → Clean Project*, desinstalar la app del dispositivo (limpia DataStore) y volver a instalar.

## v0.1.57

### Añadido
- **Los temas de reproducción ahora transforman también el contenido** (no solo el overlay de controles):
  - **Galería**: muestra siempre un paspartú editorial tipo museo (`GalleryMatFrame`, grafito + crema) alrededor de la foto/vídeo, independientemente del marco dorado.
  - **Ambiente**: contenido a sangre, sin ningún marco (minimalismo cinematográfico).
  - **Aurora Glass**: respeta el marco dorado opcional del usuario (`showPictureFrame`).
  - Así, al cambiar de tema en Ajustes el aspecto de la pantalla cambia de inmediato, no solo al abrir los controles.
- **Ajustes — secciones colapsables con fondo de color completo**: cada sección (Fotos, Videos, Música, Slideshow, Visual, Reloj, TV, Sistema) se envuelve ahora en una tarjeta `SettingsSectionCard` con el fondo del color de acento de la sección aplicado a **todas** sus opciones, y cabecera pulsable para expandir/contraer.

### Corregido
- **Al salir de pantalla completa se detiene TODO lo que se reproduce**: nuevo `SlideshowViewModel.stopSlideshow()` que pausa el motor, detiene el vídeo en curso (`stop()`) y para la música. Se invoca en el `onDispose` de `SlideshowScreen` en lugar de solo pausar.

## v0.1.56

### Añadido
- **Temas de interfaz para la pantalla de reproducción**: nuevo enum `PlaybackTheme` con tres estilos seleccionables, persistidos en DataStore (`playback_theme`) y elegibles en Ajustes → *Visual en reproducción* → "Tema de la interfaz":
  - **Aurora Glass** (por defecto): HUD inferior glassmorphism unificado con álbumes, progreso, transporte y volúmenes (el diseño previo, extraído a su propio tema).
  - **Ambiente**: minimalismo cinematográfico — barra inferior mínima centrada (anterior · play/pausa grande con glow morado · siguiente) sobre un scrim degradado, acciones discretas arriba a la derecha.
  - **Galería**: estilo museo — riel vertical de controles acoplado a la derecha y una "placa" inferior con fecha, álbum y posición del elemento actual.
- Los controles de los 3 temas comparten un único modelo de acciones (`PlaybackControlsCallbacks`) y los componentes existentes, manteniendo coherencia y el foco D-pad.

### Corregido
- **Auto-ocultado de controles roto en TV**: antes los controles solo se ocultaban si el foco estaba en el botón play/pausa; si estaba en cualquier otro control no se ocultaban nunca. Ahora se ocultan tras 6 s de inactividad (5 s en móvil) y el temporizador se reinicia con cualquier interacción (navegación de foco, click o ajuste de volumen) mediante un contador `interactionTick`.
- **Controles desaparecían a mitad de gesto en móvil**: el temporizador fijo de 5 s ocultaba los controles aunque el usuario estuviera arrastrando un slider. Ahora cualquier interacción reinicia la cuenta.
- **Estado vacío engañoso**: si no hay medios, en vez de quedarse en "Preparando fotos…" indefinidamente, muestra "No hay fotos ni vídeos. Añade carpetas en Ajustes." cuando `totalItems == 0`.
- **Reloj de reproducción**: `SimpleDateFormat`/`Locale` se creaban en cada tick del bucle (cada segundo); ahora se instancian una sola vez fuera del bucle.

### Refactor
- `SlideshowScreen` (≈630 líneas) deja de tener el overlay de controles embebido: ahora delega en `PlaybackControlsOverlay` (nuevo `PlaybackControlThemes.kt`), quedando como orquestador de estado, render de contenido, reloj y foco.

## v0.1.55

### Corregido
- **Motor — `removeItem()` re-barajaba al borrar**: al eliminar un elemento durante la reproducción se llamaba a `buildPlaylist()` internamente, lo que volvía a barajar toda la lista y cambiaba el orden del resto de la sesión de forma inesperada. Ahora se filtra directamente `shuffledItems` preservando el orden establecido al inicio de sesión.
- **Vídeo — flash negro al llamar `prepare()`**: `ExoSlideshowVideoPlayerRepository.prepare()` llamaba a `stop()` + `clearMediaItems()` justo antes de `setMediaItem()`, dejando el player en `STATE_IDLE` y causando un parpadeo negro hasta que `prepare()` completaba. Eliminadas esas dos llamadas; con `setMediaItem()` directo ExoPlayer hace un seek-to-start implícito sin interrumpir el display.
- **Música — estado `isDucked` colgado tras reanudar**: si la sesión se interrumpía mientras se reproducía un vídeo (modo pausa/app en background), el flag `isDucked` permanecía `true` al reanudar. La siguiente llamada a `setVolume()` sobrescribía `volumeBeforeVideo` con el volumen ya atenuado, bloqueando la restauración del volumen. `SlideshowMusicCoordinator.resumePlayback()` ahora llama a `music.resetDuckedState()` antes de restaurar el volumen si detecta `isDucked=true`.
- **ViewModel — `musicCoordinator.disconnect()` nunca se llamaba**: al destruirse el `SlideshowViewModel` (rotación, navegación) el `MediaController` de música no se liberaba, acumulando callbacks huérfanos. Se añade `musicCoordinator.disconnect()` en `onCleared()` antes de `slideshowVideoPlayer.release()`.
- **Transición KEN_BURNS invisible**: `initialScale = 1.0f` no produce diferencia de escala y la animación era idéntica a un fade simple. Corregido a `0.92f` para que la imagen entre ligeramente pequeña y crezca, logrando el efecto zoom visible.

### Añadido
- **`MusicPlaybackRepository.resetDuckedState()`**: nueva función de interfaz que limpia el estado de atenuación/pausa por vídeo sin restaurar el volumen (a diferencia de `onPhotoShown` que sí restaura). Implementada en `MusicPlayerController`.

## v0.1.54

### Añadido
- **Icono de aleatorio unificado**: nuevo vector `ic_shuffle_dynamic.xml` (con el diseño SVG aportado) expuesto como `ShuffleIcon`. Reemplaza todos los iconos de aleatorio previos (`Icons.Default.Shuffle` en Ajustes y `Icons.Outlined.Shuffle` en el Dashboard) para una imagen coherente en toda la app.
- **Barra lateral — scrollbar**: la barra lateral principal (`MemoriaSidebar`) muestra una barra de desplazamiento vertical (`Modifier.verticalScrollbar`) que indica visualmente cuando hay más secciones de las visibles.
- **Preferencias — volumen de vídeo**: nuevo slider "Volumen de video" dentro de la sección *Videos* (visible solo si el audio de vídeo no está silenciado). Escribe `SlideshowConfig.mediaVolume`, el mismo valor que usa el control de la reproducción → ambos controles quedan enlazados.
- **Preferencias — colores por sección**: cada cabecera de sección (`SettingsSectionHeader`) tiene ahora un color de acento propio (Fotos azul, Videos naranja, Música verde, Slideshow morado, Visual teal, Reloj ámbar, TV índigo, Sistema gris) con barra lateral de color, icono y título tintados y fondo suave, para identificar de un vistazo en qué sección estás.

### Actualizado
- **Foco — entrar al contenido con flecha derecha**: al pulsar DERECHA desde el menú lateral, el foco entra siempre en el primer elemento focable de la sección. Se logra con `focusProperties { right = contentFocus }` en el menú y un `focusRequester` sobre el `focusGroup` del contenido, que reenvía al primer hijo focable. Comportamiento seguro (el requester siempre está adjunto), sin riesgo de crash.
- **Audio enlazado en toda la app**: el volumen de música (`SlideshowViewModel.setMusicVolume`) ahora se persiste en `SlideshowConfig.musicVolume` además de aplicarse en vivo, y los sliders de música del slideshow leen `config.musicVolume`. Junto con el volumen de vídeo (`mediaVolume`), todos los controles de audio comparten la misma fuente y se reflejan en cualquier pantalla donde aparezcan.

## v0.1.53

### Corregido
- **Vídeo — flash negro al cambiar de vídeo**: el reproductor de vídeo (`SlideshowVideoPlayer`) se extrajo de `AnimatedContent` y se monta directamente cuando el ítem actual es vídeo. Al navegar de un vídeo a otro el composable no se recrea: solo cambian `uri` y `playToken`, lo que dispara `prepare()` internamente sin destruir/crear el reproductor. El shutter de `PlayerView` pasa a ser transparente (antes negro), de modo que mientras carga el nuevo vídeo se ve el fondo configurado (letterbox) en vez de negro. Efecto combinado: transiciones vídeo→vídeo sin flash negro.
- **Vídeo — primer frame con distorsión**: se añade un fade-in de 280 ms sobre el `PlayerView` desde alpha 0 hasta 1 cuando llega el primer frame (`onRenderedFirstFrame`). Esto oculta el encuadre provisional que ExoPlayer/SurfaceView calcula antes de conocer las dimensiones reales del vídeo, y la imagen aparece siempre correctamente encuadrada.

### Añadido
- **Reproducción — volumen de vídeo independiente**: nueva fila de control en el panel inferior del slideshow (aparece solo cuando el elemento actual es un vídeo y el audio no está silenciado). En TV usa `TvVolumeStepper` igual que la música; en móvil usa un `Slider`. Modifica `SlideshowConfig.mediaVolume` en tiempo real a través de `UpdateSlideshowConfigUseCase`.

### Hoja de ruta — scroll sin salto
- **Hoja de ruta — efecto de salto al navegar entre ítems**: eliminado `BringIntoViewSpec` personalizado (causaba conflictos con el sistema de foco nativo de TV produciendo doble-scroll). Sustituido por scroll controlado: cada ítem notifica su índice al ganar foco (`onFocusChanged`) y un `LaunchedEffect` llama a `animateScrollToItem` solo cuando el ítem no es visible, sin interferir con la navegación D-pad normal.

## v0.1.52

### Corregido
- **Vídeo — se quedaba "pausado" al terminar (causa real)**: el callback de fin de vídeo se registraba con `setListeners` dentro de un `DisposableEffect` cuyas claves eran lambdas que cambiaban de identidad en cada recomposición, lo que provocaba re-registros y una ventana en la que el evento `STATE_ENDED` podía perderse. Con "mostrar vídeo completo" no hay temporizador de respaldo, así que el vídeo quedaba congelado para siempre. Ahora el reproductor usa un **único listener estable** (registrado una sola vez con `rememberUpdatedState` para los callbacks) que captura fin de vídeo, errores y tamaño del vídeo; el avance/repetición al terminar es fiable.
- **Imagen de fondo persistente tras el vídeo**: la capa "underlay" (foto previa, usada para fundidos suaves entre fotos) se seguía dibujando detrás del vídeo, mostrándose en las barras del letterbox. Ahora solo se dibuja cuando el elemento actual es una imagen y se limpia al pasar a vídeo: el vídeo se muestra sobre el fondo de reproducción (negro/desenfoque), no sobre la foto.

## v0.1.51

### Corregido
- **Vídeo — al terminar se quedaba "pausado" en vez de avanzar/repetir**: cuando un vídeo terminaba y el motor navegaba al MISMO elemento (bucle con un único vídeo, o navegar al mismo `id`), el reproductor no se recomponía (misma URI en `AnimatedContent`), así que no volvía a `prepare()` y el vídeo quedaba congelado en su último frame. Se añade un `playToken` en `SlideshowState` que se incrementa en cada navegación; el reproductor de vídeo re-prepara al cambiar la URI **o** el `playToken`, de modo que un vídeo se reinicia de forma determinista aunque sea el mismo. Para listas con varios elementos el avance ya funcionaba y se mantiene.

### Verificado
- **Aleatorios (fotos, vídeos, música)**: fotos y vídeos se barajan por tipo en `buildPlaylist` (cada sesión vuelve a barajar y `beginSession` arranca en un índice aleatorio); la música se baraja antes de reproducir (v0.1.49) y suena en bucle (`REPEAT_MODE_ALL`). Comportamiento de duración de vídeo confirmado: con "mostrar vídeo completo" se reproduce entero y luego avanza; sin esa opción usa el intervalo de las fotos.

## v0.1.50

### Corregido
- **Vídeo — pantalla en negro al pausar y reanudar (solo vídeos)**: la v0.1.49 cambió el reproductor a `TextureView`, que tiene un problema conocido de ciclo de vida (se queda en negro al reanudar). Se vuelve a `SurfaceView` (recomendado para vídeo en Android TV) y la distorsión inicial que motivó el cambio se corrige ahora forzando un re-layout cuando llega el tamaño del vídeo / se pinta el primer frame (`onVideoSizeChanged` / `onRenderedFirstFrame`), además de `keepContentOnPlayerReset` para evitar parpadeos a negro al re-preparar.
- **Vídeo — transición vídeo→vídeo cortaba el siguiente**: al desmontar un slide de vídeo se llamaba a `stop()` incondicional, que podía detener el vídeo ya cargado por el slide siguiente. Ahora se usa `stopIfCurrent(uri)`: solo detiene si el vídeo cargado sigue siendo el de ese slide.

## v0.1.49

### Corregido
- **Música — el aleatorio empezaba siempre por la misma canción**: el shuffle se delegaba en ExoPlayer (`shuffleModeEnabled`), que solo afecta al "siguiente": la reproducción siempre arrancaba en el índice fijo de la lista. Ahora, si el aleatorio está activo, la lista de pistas se **baraja antes de reproducir** y se reproduce en ese orden, por lo que cada sesión empieza por una canción distinta. La detección de "lista obsoleta" pasa a comparar por conjunto de ids (no por orden) para que la lista ya barajada no se reinicie en cada chequeo.
- **Vídeo — se mostraba distorsionado hasta pausar/reanudar**: el `PlayerView` usaba `SurfaceView`, que no se puede transformar ni mezclar por alpha durante las transiciones del slideshow (`AnimatedContent`), por lo que el vídeo aparecía estirado hasta forzar un re-render. Ahora el reproductor usa `TextureView` (layout `slideshow_player_view.xml`) y se adjunta al crearse, de modo que el vídeo se muestra completo en su resolución (`RESIZE_MODE_FIT`, sin recortar) y correcto desde el primer frame, igual que las fotos.

## v0.1.48

### Corregido
- **Preferencias — carpetas mezcladas entre fotos y vídeos**: con la lista de vídeos vacía, la sección de vídeos mostraba las carpetas de fotos (y viceversa). La causa era el fallback a la clave legacy combinada `MEDIA_FOLDERS`, que se reescribe en cada guardado. Ahora solo se migra desde esa clave cuando la clave específica nunca se guardó (config antigua); una lista vacía ya no se rellena con la de la otra sección.

### Actualizado
- **Preferencias — orden de secciones**: "Música de fondo" ahora aparece justo después de "Videos" (Fotos → Videos → Música de fondo → Slideshow → Visual…).

## v0.1.47

### Corregido
- **Hoja de ruta — scroll ágil**: la animación de auto-scroll por foco se redujo de 260 ms a 90 ms. Antes la fila se "revelaba lentamente" y al subir/bajar rápido se notaba retardo; ahora responde al instante manteniendo la suavidad.
- **Chips de filtro — foco visible sobre el activo**: cuando el foco cae sobre un chip ya activo (violeta) no había forma de saber que estabas sobre él. Ahora todo chip enfocado muestra un anillo blanco de 2 dp, así se distingue siempre cuál está bajo el foco (activo o no).

## v0.1.46

### Corregido
- **Hoja de ruta — fin del "brinco" al desplazar**: el auto-scroll por foco usaba una animación tipo muelle (spring) que se sentía como un salto. Ahora se usa un `BringIntoViewSpec` propio que mantiene el desplazamiento mínimo (las filas ya visibles no se mueven y el D-pad sigue llegando a las de abajo) pero con una animación suave (tween), logrando un scroll natural como el de una lista normal.

## v0.1.45

### Corregido
- **Hoja de ruta — scroll natural (sin salto por línea)**: el espaciado entre filas se pasó de `Arrangement.spacedBy` + `contentPadding` a un **margen dentro de cada fila, fuera del área enfocable**. Así el auto-scroll por foco (bring-into-view) no sobre-desplaza y la lista se mueve de forma fluida, como una lista normal.
- **Hoja de ruta — primera fila al entrar (robusto)**: en lugar de la API experimental que provocaba crash, ahora se detecta cuándo la lista gana el foco (`onFocusChanged`) y se posiciona/enfoca la primera fila de forma segura. Entrar en la lista siempre marca la línea 1.

## v0.1.44

### Corregido
- **Crash al navegar la hoja de ruta**: se eliminó `focusProperties { enter }` (API experimental) que devolvía un `FocusRequester` no adjunto cuando la primera fila no estaba compuesta, cerrando la app. El foco inicial en la primera fila se mantiene de forma segura.
- **Salto constante al desplazar la lista**: `allItems` no estaba memoizado, así que se recreaba en cada recomposición y reiniciaba el `LaunchedEffect` que hacía `scrollToItem(0)`. Ahora está memoizado por categoría y el reposicionado al inicio solo ocurre al cambiar de filtro.
- **Foco al salir con IZQUIERDA**: ahora vuelve siempre al item del menú de la sección actual usando `focusProperties { exit }` (la API correcta para redirigir la salida de un `focusGroup`), en lugar de `left` que no se aplicaba al grupo y dejaba el foco en otra sección.

## v0.1.43

### Corregido
- **Hoja de ruta — sin "brinco" al desplazar**: la altura de cada fila ya no cambia al enfocar (peso de fuente constante y `maxLines` con elipsis), eliminando el salto al subir/bajar.
- **Hoja de ruta — foco en la primera fila al entrar**: además del foco inicial, la lista usa `focusProperties { enter }` para que al entrar (D-pad) el foco caiga siempre en la primera fila.

### Actualizado
- **Chips de filtro (Todos / Listo / Parcial / Próximamente)**: estados visuales propios de una app moderna — **activo = violeta completo** con texto blanco; **enfocado (no activo) = gris** con texto blanco; **reposo = contorno** sobre fondo claro. Transición de color suave.

## v0.1.42

### Actualizado
- **Hoja de ruta**: las filas enfocadas ahora se rellenan de **morado completo** con texto blanco (como los botones principales), en vez del fondo suave con barra lateral.
- **Foco al entrar en la hoja de ruta**: cae siempre en la **primera fila** (scroll al inicio) para un flujo orgánico.
- **Foco al salir de la lista** (D-pad izquierda): vuelve al ítem seleccionado del menú lateral (p. ej. "Hoja de ruta"), no a otra sección. Se comparte el `FocusRequester` del item seleccionado y el contenido usa `focusProperties { left = ... }`.
- **Botones de toda la app**: al enfocar se rellenan de **morado completo** con contenido blanco (consistencia global): `NostalgiaActionButton`, `DeviceActionButton`, `TvPickerChip`, `TvStepperChip` y los ítems del menú lateral.

### Añadido
- `safeClickable` / `tvClickable`: parámetro `focusScale` para que un botón pueda dibujar su propio relleno de foco sin borde ni escalado.

## v0.1.41

Auditoría completa del proyecto: corrección de errores críticos, lógicos y mejoras visuales/técnicas.

### Corregido
- **Carpetas desactivadas ahora se excluyen de verdad**: `GetSlideshowItemsUseCase`, `GetMusicTracksUseCase`, `hasCustomMediaFolders()`, las píldoras de álbum y la `mediaKey` del `SlideshowEngine` usan `activePhotoFolderUris()` / `activeVideoFolderUris()` / `activeMusicFolderUris()`. Si se desactivan todas las carpetas, vuelve a la galería del dispositivo.
- **Vídeo seguía sonando/reproduciéndose al pasar a una foto**: el `SlideshowVideoPlayer` llama a `stop()` al desmontar el slide de vídeo y libera el `PlayerView`.
- **La pausa reiniciaba el vídeo**: separado `prepare()` (solo al cambiar de URI) de play/pausa y volumen, que ya no recargan el medio.
- **Música no se atenúa si el vídeo está silenciado** (`muteVideoAudio`): no tiene sentido bajar la música por un vídeo mudo.
- **Botón de música**: ahora reproduce/pausa solo la música, sin parar el slideshow de fotos/vídeos.
- **Bucle infinito si todos los medios fallan**: contador de errores consecutivos que pausa con mensaje claro cuando toda la lista falla.
- **Borrado en Android 10+**: mensaje claro cuando el sistema requiere confirmación, sin crash.
- **Condición de carrera en DataStore**: `updateConfig` se serializa por el `Mutex` del use case.
- **Ajustes a pantalla completa**: al cambiar carpetas recarga los medios (`onMediaChanged`).

### Añadido
- **Multi-fuente de música real**: `GetMusicTracksUseCase` fusiona pistas de todas las fuentes activas (Biblioteca + Carpeta local) sin duplicados. El panel de música del Home también es multi-select.
- **Autostart más robusto (Android TV)**: `BootReceiver` con try/catch, soporte `LOCKED_BOOT_COMPLETED` / `QUICKBOOT_POWERON`, `directBootAware`; el extra `AUTO_STARTED` ya se consume y abre el slideshow a pantalla completa al arrancar.
- **Banner de permisos accionable**: botón "Conceder acceso" que solicita el permiso directamente (Home y Ajustes).
- **Reproductor de vídeo** con `AudioAttributes` de película y como singleton (`@Singleton`).

### Actualizado
- **Límites de decodificación (Coil)**: miniaturas (512px) y fondo personalizado (1920×1080) acotados para evitar OOM con fotos 4K+.
- **Hoja de ruta**: textos más grandes en TV (10-foot UI).
- **Fuentes de música no implementadas** (Spotify/YouTube/Tema) atenuadas y deshabilitadas; `SettingsSwitchItem` admite `enabled`.
- **Banner de permisos** unificado a la paleta Memoria.

## v0.1.40

### Añadido
- Hoja de ruta: foco profesional con barra lateral animada y fondo suave (sin salto al desplazar, borde fijo 1dp).
- Preferencias: toggle activar/desactivar carpetas sin eliminarlas (icono ojo); al desactivar no se muestran fotos/videos/música.
- Preferencias: botón papelera (🗑) independiente y navegable con D-pad en cada FolderChip.
- Preferencias: icono 🔀 en todas las opciones de reproducción aleatoria.
- Preferencias: fuente de música ahora es multi-select; se pueden combinar Biblioteca del dispositivo + Carpeta local.
- `SlideshowConfig`: propiedades `disabledPhotoFolderUris`, `disabledVideoFolderUris`, `disabledMusicFolderUris`, `musicSourceTypes`.

### Corregido
- Botones +/- de volumen (TvStepperChip): la Row que los contiene ahora tiene `focusGroup()`, lo que permite navegar entre ellos con D-pad.
- Galería del dispositivo atenuada visualmente para diferenciarla de las acciones principales.
- `focusGroup` import limpio; eliminado import obsoleto `hasCustomMediaFolders` (ahora es método de `SlideshowConfig`).

## v0.1.39

### Añadido
- Hoja de ruta: barra de desplazamiento vertical, chips de filtro por estado (Todos / Listo / Parcial / Próximamente), highlight con borde morado al enfocar ítems con D-pad.
- Foco estilo Netflix: al volver al sidebar, el foco se restaura automáticamente sobre el ítem seleccionado.
- `focusGroup()` en el panel de contenido de HomeScreen para restauración bidireccional de foco.

### Corregido
- Preferencias: `FolderChip` en TV es ahora una fila completa focusable (OK = quitar); en móvil mantiene el botón de papelera.
- Preferencias: muestra la ruta completa de cada carpeta debajo del nombre.
- Preferencias: secciones reorganizadas por tipo de contenido: Fotos → Videos → Slideshow → Música → Visual → Reloj → Pantalla TV → Sistema.
- Preferencias: "Usar galería del dispositivo" movida dentro de la sección Fotos.
- Preferencias: versión de app al final, no focusable, solo informativa.
- Preferencias: Spacer explícito como primer ítem para evitar recorte del texto superior.

## v0.1.38

### Corregido
- Pantalla Ajustes: título "Ajustes" recortado en la parte superior (contentPadding top = 0).
- Pantalla Ajustes: botón duplicado "Selector del sistema (fotos)" eliminado.

### Añadido
- Notas descriptivas en cada control de Ajustes: en TV aparecen al recibir foco; en móvil siempre visibles.
- Múscia aleatoria reubicada al inicio de la sección Música para mayor visibilidad.

### Actualizado
- Secciones de Ajustes reorganizadas: Contenido → Slideshow → Visual → Reloj → Música → Videos → Pantalla TV → Sistema.
- Fotos/videos aleatorios y Bucle movidos a la sección Slideshow (era "Modo reproducción").
- `SettingsSwitchItem`: comparte `InteractionSource` con `safeClickable` para detectar foco correctamente.

## v0.1.37

### Corregido
- Hoja de ruta: el listado no hacía scroll porque `LazyColumn` no tenía altura acotada. Aplicado `Modifier.fillMaxSize()` / `weight(1f)` según contexto.
- Hoja de ruta en TV: ítems ahora son focusables con D-pad para desplazamiento correcto.

## v0.1.36

### Corregido
- DataStore de ajustes y depuración centralizados en Hilt (`DataStoreModule`): una sola instancia por archivo, sin delegados duplicados.

## v0.1.35

### Corregido
- Crash al añadir carpeta: `AndroidAppDebugLogger` y `DataStoreSettingsRepository` abrían el mismo archivo DataStore (`slideshow_settings`).

## v0.1.34

### Corregido
- Compilación: import duplicado de `AppDebugLogger` en `MainActivity`.
- Compilación: listener de navegación usa `DisposableEffect` en lugar de `LaunchedEffect` + `onDispose`.

## v0.1.33

### Corregido
- Crash en Ajustes al desplazarse: claves duplicadas en `LazyColumn` cuando la misma carpeta estaba en fotos y vídeos.

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
