# Probar DynamicFrame en Google TV / Android TV

## Variante correcta

Siempre usa **`tvDebug`** (flavor `tv` + build `debug`).

| Variante | Package (debug) | Uso |
|----------|-----------------|-----|
| `tvDebug` | `com.dynamicframe.debug` | TV, Google TV, emulador TV |
| `mobileDebug` | `com.dynamicframe.debug` | Móvil / tablet touch |

La UI detecta TV con `UiModeManager` + `UI_MODE_TYPE_TELEVISION`. En emulador TV o Chromecast con Google TV entra layout sidebar + D-pad.

---

## Android Studio se queda “buscando dispositivos Wi‑Fi”

El escaneo automático a veces no funciona con TV box. **No hace falta** que Studio encuentre el TV por Wi‑Fi.

1. Empareja por consola (misma red local, PC puede ir por cable):

```powershell
cd Z:\DynamicFrame
.\pair-tv.ps1 -TvIp 192.168.0.146 -PairPort PUERTO_DEL_TV
```

2. Si tras emparejar falla `connect`, el TV muestra **otro puerto** de conexión:

```powershell
.\pair-tv.ps1 -TvIp 192.168.0.146 -PairPort PUERTO_PAIR -ConnectPort 37963
```

3. Con `adb devices` en verde, abre Android Studio → variante **tvDebug** → el TV debería aparecer arriba → **Run**.

En Studio, la entrada manual de IP a veces está en Device Manager → **+** → enlace pequeño **“Pair using pairing code”** (no en el escaneo QR). Si no la ves, usa `pair-tv.ps1`.

---

## Opción A — Emulador en Android Studio (recomendado para empezar)

1. Abre el proyecto en **`C:\DynamicFrame`** (no desde `Z:\` si usas `build-local.ps1`).
2. **Device Manager** → Create Device.
3. Categoría **TV** → elige **Android TV (1080p)** o **Google TV** si tienes la system image instalada.
4. System image: **API 34** (Google APIs).
5. Build Variants → **`tvDebug`**.
6. Run ▶ sobre el emulador TV.

### Añadir fotos y música al emulador

```text
Arrastrar archivos .jpg / .mp4 al emulador
→ se guardan en Downloads
→ Galería / MediaStore los indexa en unos segundos
```

O por ADB:

```powershell
adb push C:\ruta\foto.jpg /sdcard/Pictures/
adb push C:\ruta\cancion.mp3 /sdcard/Music/
```

En la app: **Ajustes** → conceder permisos de medios → elegir álbumes o carpetas.

---

## Opción B — Script local (Z: → C: + compilar + instalar)

Desde `Z:\DynamicFrame` o la carpeta del workspace:

```powershell
# Solo compilar APK TV
.\build-local.ps1

# Compilar, instalar en dispositivo/emulador conectado y abrir
.\build-local.ps1 -Run
```

Requisitos: Android Studio instalado (JDK embebido) y SDK en  
`%LOCALAPPDATA%\Android\Sdk`.

Con emulador ya encendido o TV por ADB:

```powershell
adb devices
```

---

## Opción C — Google TV / Chromecast físico

1. TV: **Ajustes → Sistema → Acerca de** → pulsa **Compilación** 7 veces.
2. **Opciones de desarrollador** → **Depuración ADB** (inalámbrica o USB).
3. Anota IP:puerto (ej. `192.168.1.50:5555`).

```powershell
adb connect 192.168.1.50:5555
adb devices
adb install -r C:\DynamicFrame\app\build\outputs\apk\tv\debug\app-tv-debug.apk
adb shell am start -n com.dynamicframe.debug/com.dynamicframe.MainActivity
```

La app tiene categoría **LEANBACK_LAUNCHER** para Android TV.

### Google TV / Chromecast: no la veo en inicio

**Es normal.** Las apps instaladas por USB o ADB (fuera de Play Store) **casi nunca aparecen en la fila de inicio** del Google TV.

**Dónde está:**

1. **Ajustes → Aplicaciones → Ver todas las aplicaciones** → busca **Dynamic Frame**
2. Play Store en el TV → instala **Sideload Launcher** → ahí listan las apps sideload
3. Con ADB: `adb shell am start -n com.dynamicframe.debug/com.dynamicframe.MainActivity`

Script rápido desde el proyecto (instala + abre + comprueba):

```powershell
.\install-tv.ps1 -TvIp 192.168.0.146
```

---

## Checklist de prueba en TV (anota qué falla)

### Navegación D-pad

- [ ] Sidebar: Slideshow / Álbumes / Música / Ajustes — foco visible y OK entra
- [ ] Botón **Pantalla completa** / Play slideshow
- [ ] Thumbnails en fila — izquierda/derecha + OK
- [ ] Slideshow fullscreen: OK o Centro = pausar / mostrar controles
- [ ] Ajustes: switches, sliders, dropdowns (transición, intervalo, volumen)

### Reproducción

- [ ] Fotos cargan (no pantalla negra)
- [ ] Transiciones se ven fluidas
- [ ] Vídeo reproduce y al terminar pasa a la siguiente
- [ ] Música suena en panel y en fullscreen
- [ ] Pausar para fotos + música + vídeo a la vez
- [ ] Ducking o pausa de música cuando hay vídeo (según ajuste)

### Permisos y contenido

- [ ] Al abrir **no** pide permisos (solo en Ajustes al elegir fuentes)
- [ ] Álbumes listados tras conceder permiso
- [ ] Carpeta SAF (USB simulado o carpeta del emulador)

### TV específico

- [ ] Textos legibles a 2–3 m (sidebar, reloj)
- [ ] Reloj grande / compacto
- [ ] Modo inmersivo sin reloj ni controles
- [ ] Back sale de fullscreen sin crash
- [ ] Volver a Home: música/slideshow siguen vivos (no reset total)

---

## Problemas frecuentes

| Síntoma | Causa probable |
|---------|----------------|
| App no aparece en launcher TV | Instalaste `mobileDebug` o APK sin `LEANBACK_LAUNCHER` |
| “No hay fotos” | Sin permisos o emulador vacío — añade media a Pictures |
| No se oye música | Sin archivos en Music o permiso AUDIO; revisa volumen en Ajustes |
| Controles no responden | Foco — prueba con mando/emulador D-pad, no ratón solo |
| Build falla “No Java” | Instala Android Studio o define `JAVA_HOME` al JBR |

---

## Reportar correcciones

Anota por pantalla: **qué hiciste**, **qué esperabas**, **qué pasó**.  
Las mejoras pendientes están en [ROADMAP.md](./ROADMAP.md).
