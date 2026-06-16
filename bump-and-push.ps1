# Sube PATCH + VERSION_CODE (solo al entregar cambios, no al compilar)
param(
    [switch]$Push
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    foreach ($j in @(
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "${env:ProgramFiles(x86)}\Android\Android Studio\jbr"
    )) {
        if (Test-Path "$j\bin\java.exe") { $env:JAVA_HOME = $j; break }
    }
}

Set-Location $root
Write-Host "=== bumpVersion ===" -ForegroundColor Cyan
& .\gradlew.bat :app:bumpVersion
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$props = Get-Content "app\version.properties" | Where-Object { $_ -match '^VERSION_' }
$ver = ($props | Where-Object { $_ -match 'VERSION_MAJOR' }) -replace '.*=', ''
$mid = ($props | Where-Object { $_ -match 'VERSION_MIDDLE' }) -replace '.*=', ''
$patch = ($props | Where-Object { $_ -match 'VERSION_PATCH' }) -replace '.*=', ''
$tag = "v$ver.$mid.$patch"
Write-Host "Nueva version: $tag" -ForegroundColor Green

if ($Push) {
    Write-Host "Commit + push + tag..." -ForegroundColor Yellow
    git add -A
    git -c user.name="onlyeyes" -c user.email="onlyeyes@users.noreply.github.com" commit -m "release: $tag — version visible en UI"
    git push origin HEAD
    git tag $tag 2>$null
    git push origin $tag
    Write-Host "Publicado: $tag" -ForegroundColor Green
}
