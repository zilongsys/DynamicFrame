# DynamicFrame - Sincroniza Z: -> C: y compila (igual idea que AutoCheck)
# Uso:
#   .\build-local.ps1
#   .\build-local.ps1 -Run          # compila tvDebug, instala y abre en dispositivo/emulador
#   .\build-local.ps1 -Run -Flavor mobile

param(
    [switch]$Run,
    [ValidateSet("tv", "mobile")]
    [string]$Flavor = "tv"
)

$ErrorActionPreference = "Stop"

$dst = "C:\DynamicFrame"

# Origen: carpeta compartida Z:, aunque ejecutes el script desde C:
if (Test-Path "Z:\DynamicFrame\settings.gradle.kts") {
    $src = "Z:\DynamicFrame"
} else {
    $src = $PSScriptRoot
}

Write-Host ""
Write-Host "=== DynamicFrame: build local ===" -ForegroundColor Cyan
Write-Host "Origen : $src"
Write-Host "Destino: $dst"
Write-Host ""

# Java (Android Studio trae su propio JDK)
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaCandidates = @(
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "${env:ProgramFiles(x86)}\Android\Android Studio\jbr",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
    )
    foreach ($j in $javaCandidates) {
        if (Test-Path "$j\bin\java.exe") {
            $env:JAVA_HOME = $j
            break
        }
    }
}
if (-not $env:JAVA_HOME) {
    Write-Host "ERROR: No se encontro Java. Instala Android Studio o define JAVA_HOME." -ForegroundColor Red
    exit 1
}
Write-Host "Java   : $env:JAVA_HOME" -ForegroundColor DarkGray

if (-not (Test-Path $dst)) {
    New-Item -ItemType Directory -Path $dst | Out-Null
}

$srcNorm = (Resolve-Path $src).Path.TrimEnd('\')
$dstNorm = (Resolve-Path $dst).Path.TrimEnd('\')

if ($srcNorm -ne $dstNorm) {
    Write-Host "Sincronizando codigo (no toca cache Gradle en C:)..." -ForegroundColor Yellow
    # /XD excluye carpetas: no se copian desde Z: ni se borran en C:
    # Gradle descargado queda en C:\Users\...\ .gradle\ (global, fuera del proyecto)
    $robocopyArgs = @(
        $src, $dst,
        "/MIR",
        "/XD", "build", "app\build", ".gradle", ".idea",
        "/XF", "local.properties",
        "/NFL", "/NDL", "/NJH", "/NJS", "/NC", "/NS"
    )
    & robocopy @robocopyArgs | Out-Null
    if ($LASTEXITCODE -gt 7) {
        Write-Host "ERROR: robocopy fallo con codigo $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "Sincronizacion completada." -ForegroundColor Green
} else {
    Write-Host "Origen y destino iguales, se omite la copia." -ForegroundColor DarkGray
}

Set-Location $dst

$sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
if (-not (Test-Path $sdk)) {
    Write-Host "ERROR: No se encontro Android SDK en $sdk" -ForegroundColor Red
    exit 1
}
$sdkPath = $sdk -replace '\\', '/'
"sdk.dir=$sdkPath" | Set-Content -Path "local.properties" -Encoding ASCII
Write-Host "SDK    : $sdk" -ForegroundColor DarkGray

$assembleTask = if ($Flavor -eq "mobile") { "assembleMobileDebug" } else { "assembleTvDebug" }
$apkRel = if ($Flavor -eq "mobile") {
    "app\build\outputs\apk\mobile\debug\app-mobile-debug.apk"
} else {
    "app\build\outputs\apk\tv\debug\app-tv-debug.apk"
}

Write-Host ""
Write-Host "Compilando ($assembleTask)..." -ForegroundColor Yellow
& .\gradlew.bat $assembleTask
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $Run) { exit 0 }

$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    Write-Host "ERROR: adb no encontrado en $adb" -ForegroundColor Red
    exit 1
}

$apk = Join-Path $dst $apkRel
if (-not (Test-Path $apk)) {
    Write-Host "ERROR: APK no generado: $apk" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Instalando en dispositivo..." -ForegroundColor Yellow
& $adb devices
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Abriendo DynamicFrame..." -ForegroundColor Green
& $adb shell am start -n com.dynamicframe.debug/com.dynamicframe.MainActivity
exit $LASTEXITCODE
