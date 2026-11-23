# Build Script for Intelligent Daily Schedule Optimizer
$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$srcDir = Join-Path $projectRoot "src"
$binDir = Join-Path $projectRoot "bin"
$libDir = Join-Path $projectRoot "lib"
$jarPath = Join-Path $libDir "mysql-connector-j.jar"

if (-not (Test-Path $binDir)) {
    New-Item -ItemType Directory -Path $binDir | Out-Null
}

# Auto-download MySQL Connector if missing
if (-not (Test-Path $jarPath)) {
    Write-Host "[INFO] MySQL Connector JAR not found in lib/. Downloading..." -ForegroundColor Yellow
    $url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"
    try {
        Invoke-WebRequest -Uri $url -OutFile $jarPath
        Write-Host "[INFO] Downloaded mysql-connector-j.jar successfully!" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Could not download MySQL Connector automatically. Please check your internet connection." -ForegroundColor Red
        exit 1
    }
}

# Locate javac binary
$javac = "javac"
$jdkPath = "C:\Program Files\Java\jdk-27.0.1.1\bin\javac.exe"
if (Test-Path $jdkPath) {
    $javac = $jdkPath
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    $javac = "$env:JAVA_HOME\bin\javac.exe"
}

Write-Host "Compiling Java source files using $javac..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path $srcDir -Filter *.java -Recurse | Select-Object -ExpandProperty FullName

& $javac -encoding UTF-8 -cp "$jarPath;$srcDir" -d $binDir $javaFiles

if ($LASTEXITCODE -eq 0 -or (Test-Path (Join-Path $binDir "com\scheduler\Main.class"))) {
    Write-Host "[SUCCESS] Build completed! Classes output to bin/" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    exit 1
}
