# Instala app-tv-debug.apk y comprueba si quedo en el TV
# Uso:
#   .\install-tv.ps1 -TvIp 192.168.0.146 -TvPort 37963
#   .\install-tv.ps1 -DeviceSerial 192.168.0.146:37963

param(
    [string]$TvIp = "",
    [int]$TvPort = 37963,
    [string]$DeviceSerial = "",
    [string]$ApkPath = ""
)

$ErrorActionPreference = "Stop"
$pkg = "com.dynamicframe.debug"

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

function Find-Apk {
    param([string]$Explicit)
    if ($Explicit -and (Test-Path $Explicit)) { return (Resolve-Path $Explicit).Path }
    $candidates = @(
        "C:\DynamicFrame\app\build\outputs\apk\tv\debug\app-tv-debug.apk",
        (Join-Path $PSScriptRoot "app\build\outputs\apk\tv\debug\app-tv-debug.apk")
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return (Resolve-Path $c).Path }
    }
    return $null
}

function Get-ConnectedDevices {
    param([string]$Adb)
    $result = @()
    $lines = & $Adb devices | Select-Object -Skip 1
    foreach ($line in $lines) {
        if ($line -match '^(\S+)\s+(device|offline|unauthorized)\s*$') {
            if ($Matches[2] -eq "device") {
                $result += $Matches[1]
            }
        }
    }
    return $result
}

function Resolve-DeviceSerial {
    param(
        [string[]]$Devices,
        [string]$TvIp,
        [int]$TvPort,
        [string]$ExplicitSerial
    )
    if ($ExplicitSerial) {
        if ($Devices -contains $ExplicitSerial) { return $ExplicitSerial }
        Write-Host "ERROR: El serial '$ExplicitSerial' no esta en la lista de dispositivos." -ForegroundColor Red
        exit 1
    }

    if ($TvIp) {
        $expected = "${TvIp}:${TvPort}"
        if ($Devices -contains $expected) { return $expected }
        $byIp = $Devices | Where-Object { $_ -like "${TvIp}:*" -or $_ -eq $TvIp }
        if ($byIp.Count -eq 1) { return $byIp[0] }
        if ($byIp.Count -gt 1) {
            Write-Host "ERROR: Varios dispositivos con la misma IP. Usa -DeviceSerial:" -ForegroundColor Red
            $byIp | ForEach-Object { Write-Host "  $_" }
            exit 1
        }
    }

    if ($Devices.Count -eq 1) { return $Devices[0] }

    Write-Host ""
    Write-Host "ERROR: Hay mas de un dispositivo conectado. Especifica el TV:" -ForegroundColor Red
    $Devices | ForEach-Object { Write-Host "  $_" }
    Write-Host ""
    Write-Host "Ejemplo:" -ForegroundColor Yellow
    Write-Host "  .\install-tv.ps1 -DeviceSerial 192.168.0.146:37963"
    Write-Host ""
    Write-Host "Para desconectar emuladores u otros:" -ForegroundColor DarkGray
    Write-Host "  adb disconnect"
    Write-Host "  adb -s emulator-5554 emu kill"
    exit 1
}

Write-Host ""
Write-Host "=== DynamicFrame: instalar en TV ===" -ForegroundColor Cyan

$adb = Find-Adb
if (-not $adb) {
    Write-Host "ERROR: No se encontro adb.exe" -ForegroundColor Red
    exit 1
}
Write-Host "adb    : $adb" -ForegroundColor DarkGray

$apk = Find-Apk -Explicit $ApkPath
if (-not $apk) {
    Write-Host "ERROR: No hay APK. Compila antes: .\build-local.ps1" -ForegroundColor Red
    exit 1
}
Write-Host "apk    : $apk" -ForegroundColor DarkGray

if ($TvIp) {
    Write-Host ""
    Write-Host "Conectando a ${TvIp}:${TvPort}..." -ForegroundColor Yellow
    & $adb connect "${TvIp}:${TvPort}" | Out-Null
}

Write-Host ""
Write-Host "Dispositivos:" -ForegroundColor Yellow
& $adb devices

$devices = Get-ConnectedDevices -Adb $adb
if ($devices.Count -eq 0) {
    Write-Host "ERROR: Ningun dispositivo en estado 'device'." -ForegroundColor Red
    exit 1
}

$serial = Resolve-DeviceSerial -Devices $devices -TvIp $TvIp -TvPort $TvPort -ExplicitSerial $DeviceSerial
Write-Host ""
Write-Host "Usando : $serial" -ForegroundColor Green

Write-Host ""
Write-Host "Instalando..." -ForegroundColor Yellow
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: La instalacion fallo (codigo $LASTEXITCODE)" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Comprobando paquete $pkg ..." -ForegroundColor Yellow
$installed = & $adb -s $serial shell pm path $pkg 2>$null
if (-not $installed) {
    Write-Host "ERROR: El paquete no aparece tras instalar." -ForegroundColor Red
    exit 1
}
Write-Host "OK     : $installed" -ForegroundColor Green

Write-Host ""
Write-Host "Abriendo app..." -ForegroundColor Yellow
& $adb -s $serial shell am start -n "$pkg/com.dynamicframe.MainActivity"

Write-Host ""
Write-Host "Si no la ves en inicio: Ajustes -> Aplicaciones -> Dynamic Frame" -ForegroundColor Cyan
Write-Host ""
