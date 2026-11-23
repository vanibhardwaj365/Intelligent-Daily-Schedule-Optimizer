# Intelligent Daily Schedule Optimizer - 1-Click Runner for PowerShell
$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
Set-Location $projectRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "     INTELLIGENT DAILY SCHEDULE OPTIMIZER - 1-CLICK RUN    " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Locate Java
$javaCmd = "java"
$jdkJava = "C:\Program Files\Java\jdk-27.0.1.1\bin\java.exe"
if (Test-Path $jdkJava) {
    $javaCmd = $jdkJava
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaCmd = "$env:JAVA_HOME\bin\java.exe"
}

# 2. Check / Build classes if not already compiled
$mainClass = Join-Path $projectRoot "bin\com\scheduler\Main.class"
if (-not (Test-Path $mainClass)) {
    Write-Host "[1/2] Compiling Java classes..." -ForegroundColor Yellow
    & powershell -ExecutionPolicy Bypass -File (Join-Path $projectRoot "build.ps1")
} else {
    Write-Host "[1/2] Compiled classes verified in bin/" -ForegroundColor Green
}

# 3. Launch Java Application
Write-Host "[2/2] Starting server at http://localhost:8080 ..." -ForegroundColor Cyan
Write-Host "      Press Ctrl+C in terminal to stop the server.`n" -ForegroundColor Gray

# Open browser
Start-Process "http://localhost:8080/login.html"

# Run Java Main
$jarPath = Join-Path $projectRoot "lib\mysql-connector-j.jar"
$binPath = Join-Path $projectRoot "bin"

& $javaCmd -cp "$binPath;$jarPath" com.scheduler.Main
