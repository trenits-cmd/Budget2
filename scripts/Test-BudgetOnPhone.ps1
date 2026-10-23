$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Stop-WithMessage {
    param([string]$Message)
    Write-Host ''
    Write-Host $Message -ForegroundColor Red
    Write-Host ''
    Read-Host 'Enter zum Schliessen'
    exit 1
}

$javaHome = 'C:\Program Files\Android\Android Studio\jbr'
if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $javaHome = $env:JAVA_HOME
    } else {
        Stop-WithMessage 'Java nicht gefunden. Bitte Android Studio installieren oder JAVA_HOME setzen.'
    }
}
$env:JAVA_HOME = $javaHome

$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$env:ANDROID_HOME = $androidHome
$adb = Join-Path $androidHome 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) {
    Stop-WithMessage "adb nicht gefunden: $adb"
}

Clear-Host
Write-Host 'Budget2 - Handy-Test' -ForegroundColor Cyan
Write-Host "Projekt: $repoRoot"
Write-Host ''

Write-Host '[1/3] Baue Debug-APK ...' -ForegroundColor Yellow
& (Join-Path $repoRoot 'gradlew.bat') ':app:assembleDebug' '--console=plain'
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage 'Build fehlgeschlagen.'
}

$apk = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) {
    Stop-WithMessage "APK nicht gefunden: $apk"
}

Write-Host ''
Write-Host '[2/3] Suche Handy ...' -ForegroundColor Yellow
$deviceLines = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' })
if ($deviceLines.Count -eq 0) {
    Stop-WithMessage 'Kein Handy gefunden. USB-Kabel anschliessen oder WLAN-ADB verbinden.'
}
$serial = ($deviceLines[0] -split '\s+')[0]
Write-Host "Gefunden: $serial"

Write-Host ''
Write-Host '[3/3] Installiere und starte ...' -ForegroundColor Yellow
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage 'Installation fehlgeschlagen. Ist die App mit einem anderen Key signiert?'
}

& $adb -s $serial shell am force-stop com.budget2.app | Out-Null
& $adb -s $serial shell am start -n com.budget2.app/.MainActivity | Out-Null
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage 'App konnte nicht gestartet werden.'
}

Write-Host ''
Write-Host 'Fertig. Die App ist auf dem Handy geoeffnet.' -ForegroundColor Green
Start-Sleep -Seconds 2
