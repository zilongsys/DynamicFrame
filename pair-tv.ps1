# Empareja y conecta el TV por red local (sin usar el escaneo Wi-Fi de Android Studio)
# Uso:
#   .\pair-tv.ps1 -TvIp 192.168.0.146 -PairPort 45678 -ConnectPort 37963
# El TV muestra puerto + codigo en: Opciones desarrollador -> Emparejar con codigo

param(
    [Parameter(Mandatory = $true)]
    [string]$TvIp,
    [Parameter(Mandatory = $true)]
    [int]$PairPort,
    [int]$ConnectPort = 0
)

$ErrorActionPreference = "Stop"

function Find-Adb {
    $paths = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe",
        "C:\platform-tools\adb.exe"
    )
    foreach ($p in $paths) {
        if (Test-Path $p) { return $p }
    }
    return $null
}

$adb = Find-Adb
if (-not $adb) {
    Write-Host "ERROR: No se encontro adb.exe" -ForegroundColor Red
    Write-Host "Instala Android SDK Platform-Tools o descarga:" -ForegroundColor Yellow
    Write-Host "  https://developer.android.com/tools/releases/platform-tools"
    Write-Host "  Descomprime en C:\platform-tools"
    exit 1
}

$portConnect = if ($ConnectPort -gt 0) { $ConnectPort } else { $PairPort }

Write-Host ""
Write-Host "=== Emparejar TV por red local ===" -ForegroundColor Cyan
Write-Host "adb    : $adb"
Write-Host "TV     : ${TvIp}:${PairPort} (emparejar)"
Write-Host ""
Write-Host "En el TV: Opciones desarrollador -> Emparejar dispositivo con codigo" -ForegroundColor Yellow
Write-Host "Cuando pida el codigo, escribe los 6 digitos que ves en el TV." -ForegroundColor Yellow
Write-Host ""

& $adb pair "${TvIp}:${PairPort}"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: emparejamiento fallo" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Conectando ${TvIp}:${portConnect} ..." -ForegroundColor Yellow
& $adb connect "${TvIp}:${portConnect}"
& $adb devices

Write-Host ""
Write-Host "Si ves 'device' arriba, vuelve a Android Studio:" -ForegroundColor Green
Write-Host "  1) Build Variants -> tvDebug"
Write-Host "  2) Arriba elige el dispositivo (puede tardar unos segundos)"
Write-Host "  3) Run"
Write-Host ""
Write-Host "Si el connect falla, mira en el TV otro puerto de CONEXION (distinto al de emparejar)" -ForegroundColor DarkGray
Write-Host "  .\pair-tv.ps1 -TvIp $TvIp -PairPort PUERTO_PAIR -ConnectPort PUERTO_CONEXION"
Write-Host ""
