@echo off
:: ============================================================
::  Behavioral Biometrics Analysis Tool — Build & Run (Windows)
:: ============================================================
echo Compiling all sources...
javac -encoding UTF-8 *.java

if %ERRORLEVEL% == 0 (
    echo Compilation successful. Launching dashboard...
    java SecurityDashboard
) else (
    echo Compilation failed. Check errors above.
    pause
)
