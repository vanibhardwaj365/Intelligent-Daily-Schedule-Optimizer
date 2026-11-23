@echo off
setlocal enabledelayedexpansion
title Intelligent Daily Schedule Optimizer - Runner
cd /d "%~dp0"

echo ============================================================
echo      INTELLIGENT DAILY SCHEDULE OPTIMIZER - 1-CLICK RUN
echo ============================================================

:: 1. Detect Java Compiler & Runtime
set "JAVA_CMD=java"
set "JAVAC_CMD=javac"

if exist "C:\Program Files\Java\jdk-27.0.1.1\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Java\jdk-27.0.1.1\bin\javac.exe"
    set "JAVA_CMD=C:\Program Files\Java\jdk-27.0.1.1\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

:: 2. Check / Auto-download MySQL Connector JAR if missing
if not exist "lib" mkdir lib
if not exist "lib\mysql-connector-j.jar" (
    echo [INFO] Downloading MySQL Connector Driver JAR into lib/...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar', 'lib\mysql-connector-j.jar')"
    if not exist "lib\mysql-connector-j.jar" (
        echo [ERROR] Could not download MySQL Connector. Please verify your internet connection.
        pause
        exit /b 1
    )
    echo [INFO] MySQL Connector Driver ready.
)

:: 3. Compile Java Source Code
if not exist "bin" mkdir bin
if not exist "bin\com\scheduler\Main.class" (
    echo [INFO] Compiling Java classes...
    powershell -ExecutionPolicy Bypass -File "build.ps1"
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Build compilation failed.
        pause
        exit /b %ERRORLEVEL%
    )
)

:: 4. Launch the Java Web Server
echo [INFO] Starting Schedule Optimizer Server on http://localhost:8080 ...
echo [INFO] Opening default browser...
start "" "http://localhost:8080/login.html"

"%JAVA_CMD%" -cp "bin;lib\mysql-connector-j.jar" com.scheduler.Main
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [NOTE] Server terminated.
    pause
)
