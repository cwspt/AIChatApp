param(
  [switch]$Clean
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$env:JAVA_HOME = "D:\Projects\Personal\AndroidApps\.devtools\android\jdk\jdk-17.0.18+8"
$env:ANDROID_HOME = "D:\Projects\Personal\AndroidApps\.devtools\android\sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = "D:\Projects\Personal\AndroidApps\.gradle-user-home"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"

function Fail($message) {
  Write-Error $message
  exit 1
}

function Read-Properties($path) {
  $result = @{}
  if (-not (Test-Path -LiteralPath $path)) {
    return $result
  }
  Get-Content -LiteralPath $path | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#") -or -not $line.Contains("=")) {
      return
    }
    $index = $line.IndexOf("=")
    $key = $line.Substring(0, $index).Trim()
    $value = $line.Substring($index + 1).Trim()
    $result[$key] = $value
  }
  return $result
}

function Get-SigningValue($properties, $key) {
  $releaseKey = "release.$key"
  $gradlePropertyName = "aichat.release.$key"
  $envName = "AICHAT_RELEASE_$($key.ToUpperInvariant())"
  if ($properties.ContainsKey($releaseKey) -and -not [string]::IsNullOrWhiteSpace($properties[$releaseKey])) {
    return $properties[$releaseKey]
  }
  if ($properties.ContainsKey($key) -and -not [string]::IsNullOrWhiteSpace($properties[$key])) {
    return $properties[$key]
  }
  if (-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($envName))) {
    return [Environment]::GetEnvironmentVariable($envName)
  }
  return $null
}

if (-not (Test-Path -LiteralPath ".\gradlew.bat")) {
  Fail "Missing Gradle wrapper: $repoRoot\gradlew.bat"
}
if (-not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) {
  Fail "Missing JAVA_HOME JDK: $env:JAVA_HOME"
}
if (-not (Test-Path -LiteralPath $env:ANDROID_HOME)) {
  Fail "Missing Android SDK: $env:ANDROID_HOME"
}

$properties = @{}
foreach ($file in @("release-signing.properties", "keystore.properties")) {
  $path = Join-Path $repoRoot $file
  $fileProperties = Read-Properties $path
  foreach ($key in $fileProperties.Keys) {
    $properties[$key] = $fileProperties[$key]
  }
}

$storeFile = Get-SigningValue $properties "storeFile"
$storePassword = Get-SigningValue $properties "storePassword"
$keyAlias = Get-SigningValue $properties "keyAlias"
$keyPassword = Get-SigningValue $properties "keyPassword"

if ([string]::IsNullOrWhiteSpace($storeFile) -or
    [string]::IsNullOrWhiteSpace($storePassword) -or
    [string]::IsNullOrWhiteSpace($keyAlias) -or
    [string]::IsNullOrWhiteSpace($keyPassword)) {
  Fail "Release signing is incomplete. Provide storeFile/storePassword/keyAlias/keyPassword via release-signing.properties, keystore.properties, or AICHAT_RELEASE_* environment variables."
}

$resolvedStoreFile = if ([System.IO.Path]::IsPathRooted($storeFile)) {
  $storeFile
} else {
  Join-Path $repoRoot $storeFile
}
if (-not (Test-Path -LiteralPath $resolvedStoreFile)) {
  Fail "Release keystore file does not exist: $resolvedStoreFile"
}

$tasks = @(":app:assembleRelease")
if ($Clean) {
  $tasks = @("clean") + $tasks
}

Write-Host "Building signed release APK..."
Write-Host "Project: $repoRoot"
Write-Host "Keystore: $resolvedStoreFile"
Write-Host "Output directory: $repoRoot\app\build\outputs\apk\release"

& .\gradlew.bat @tasks --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

$releaseDir = Join-Path $repoRoot "app\build\outputs\apk\release"
$apks = Get-ChildItem -LiteralPath $releaseDir -Filter "*.apk" -ErrorAction SilentlyContinue |
  Sort-Object LastWriteTime -Descending

if (-not $apks) {
  Fail "Release build completed but no APK was found in $releaseDir"
}

Write-Host ""
Write-Host "Release APK output:"
$apks | Select-Object FullName, Length, LastWriteTime | Format-Table -AutoSize
