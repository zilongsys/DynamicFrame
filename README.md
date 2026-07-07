# 🖼️ Dynamic Frame — Marco Digital Dinámico para Android TV

App de marco digital para Android TV y móvil. Muestra fotos y videos de tus álbumes
con música de fondo, reloj y transiciones animadas.

---

## ✨ Funciones (Fase 1)

- **Slideshow** de fotos y videos desde almacenamiento local
- **Música de fondo** con archivos de audio locales (MP3, FLAC, AAC...)
- **Reloj y fecha** superpuestos, configurable posición
- **Transiciones** animadas: Fade, Slide, Zoom, Dissolve
- **Configuración completa**: intervalo, volumen, álbumes, shuffle
- Compatible con **Android TV** (navegación D-pad) y **móvil** (touch)
- **Pantalla siempre encendida** — ideal como marco en la pared
- **Inicio automático** al arrancar el dispositivo (opcional)

---

## 🗂️ Estructura del proyecto

```
DynamicFrame/
├── app/src/main/java/com/dynamicframe/
│   ├── MainActivity.kt               ← Detecta TV vs móvil automáticamente
│   ├── DynamicFrameApp.kt            ← Hilt Application class
│   ├── di/
│   │   └── RepositoryModule.kt       ← Inyección de dependencias
│   ├── domain/
│   │   ├── model/Models.kt           ← Modelos de datos
│   │   ├── repository/Repositories.kt ← Interfaces
│   │   └── usecase/UseCases.kt       ← Casos de uso
│   ├── data/
│   │   ├── local/
│   │   │   ├── LocalMediaRepository.kt  ← MediaStore: fotos/videos
│   │   │   ├── LocalMusicRepository.kt  ← MediaStore: música
│   │   │   └── DataStoreSettingsRepository.kt ← Configuración persistente
│   │   ├── player/
│   │   │   ├── MusicPlaybackService.kt  ← Servicio Media3 en background
│   │   │   └── MusicPlayerController.kt ← Controlador de reproducción
│   │   └── receiver/
│   │       └── BootReceiver.kt          ← Auto-arranque
│   └── presentation/
│       ├── Navigation.kt
│       ├── slideshow/
│       │   ├── SlideshowEngine.kt    ← Motor del slideshow (timer, orden, transiciones)
│       │   ├── SlideshowViewModel.kt
│       │   └── SlideshowScreen.kt    ← UI principal
│       └── settings/
│           ├── SettingsViewModel.kt
│           └── SettingsScreen.kt     ← UI de configuración
```

---

## 🚀 Compilar (igual que AutoCheck)

AutoCheck **tampoco compila en Z:**. El script copia a `C:\autocheck` y `flutter run` se ejecuta desde ahi.
DynamicFrame funciona igual: copia a `C:\DynamicFrame` y compila desde ahi.

1. Edita en **`Z:\DynamicFrame`**
2. En PowerShell desde **`Z:\DynamicFrame`**:
   ```powershell
   .\build-local.ps1 -Run
   ```
   (Abre antes un emulador: Android Studio → Device Manager)

3. **Run en Android Studio:** abre **`C:\DynamicFrame`** → Sync Gradle → variante **tvDebug** → elige dispositivo arriba → botón Run ▶

No abras `Z:\DynamicFrame` en Android Studio.

### Emuladores recomendados

| Para probar... | Usa... |
|---|---|
| Versión móvil | Pixel 7 API 34 |
| Versión TV | Android TV 1080p API 34 |

---

## 📱 Instalar en Android TV via ADB

```bash
# 1. Activa depuración ADB en el TV:
#    Ajustes → Acerca del dispositivo → Compilación (x7) → Opciones de desarrollador → ADB inalámbrico

# 2. Conectar (reemplaza IP por la de tu TV)
adb connect 192.168.1.100:5555

# 3. Verificar
adb devices

# 4. Instalar el APK
adb install -r app/build/outputs/apk/tv/debug/app-tv-debug.apk

# 5. Lanzar directamente
adb shell am start -n com.dynamicframe.debug/com.dynamicframe.MainActivity
```

### Instalar via USB drive / explorador de archivos

1. Compila el APK: **Build → Build Bundle(s)/APK(s) → Build APK(s)**
2. Copia el APK a un USB
3. En el TV: instala **FX File Explorer** o **Solid Explorer** desde Play Store
4. Navega al USB y abre el APK

---

## 🏗️ Build flavors

El proyecto tiene dos variantes (flavors):

| Flavor | ID | Descripción |
|---|---|---|
| `tv` | `com.dynamicframe` | Optimizado para TV, banner para launcher |
| `mobile` | `com.dynamicframe.debug` | Para pruebas en móvil |

Para seleccionar el flavor en Android Studio:
**Build Variants** (panel izquierdo) → selecciona `tvDebug` o `mobileDebug`

---

## 🗺️ Roadmap

- **[`docs/PRODUCT_ROADMAP.md`](docs/PRODUCT_ROADMAP.md)** — análisis completo, brechas, comparativa Nixplay, orden de implementación
- **[`docs/ROADMAP.md`](docs/ROADMAP.md)** — índice corto con próximos pasos

Guía para probar en Google TV / emulador TV:

**[`docs/TV_TESTING.md`](docs/TV_TESTING.md)**

---

## 🔧 Dependencias principales

| Librería | Uso |
|---|---|
| **Jetpack Compose + TV Compose** | UI |
| **Media3 / ExoPlayer** | Reproducción de video y audio |
| **Hilt** | Inyección de dependencias |
| **DataStore** | Configuración persistente |
| **Coil** | Carga de imágenes |
| **MediaStore** | Acceso a fotos/videos/música locales |

---

## 📝 Notas

- La app **mantiene la pantalla encendida** mientras está en primer plano (FLAG_KEEP_SCREEN_ON)
- En **Android TV**, la navegación es 100% por D-pad — no se necesita touch
- La detección TV vs móvil es automática via `UiModeManager`
- Los permisos de medios se piden al configurar fuentes (Ajustes), no al abrir la app
