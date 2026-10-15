@echo off
setlocal
title AppBlocker Launcher

REM Self-elevate to Administrator (needed to edit the hosts file and browser policies).
net session >nul 2>&1
if %errorlevel% neq 0 (
  echo Asking for administrator permission...
  powershell -NoProfile -Command "Start-Process -Verb RunAs -FilePath '%~f0'"
  exit /b
)

REM Always work in this script's own folder, wherever it was extracted.
cd /d "%~dp0"

echo ==================================================
echo   AppBlocker - starting up
echo ==================================================
echo.

REM 0) Node.js must be installed.
where node >nul 2>&1
if %errorlevel% neq 0 (
  echo [PROBLEM] Node.js is not installed on this computer.
  echo.
  echo   1. Go to https://nodejs.org and download the "LTS" version.
  echo   2. Install it, clicking Next on every screen.
  echo   3. Double-click this run file again.
  echo.
  pause
  exit /b 1
)

REM 1) Install dependencies if ANY of them are missing (Electron OR Supabase, etc).
set NEED_INSTALL=
if not exist "node_modules\electron\package.json" set NEED_INSTALL=1
if not exist "node_modules\@supabase\supabase-js\package.json" set NEED_INSTALL=1
if not exist "node_modules\ws\package.json" set NEED_INSTALL=1
if defined NEED_INSTALL (
  echo Installing / updating what AppBlocker needs. This can take a few minutes...
  call npm install --no-audit --no-fund
  if errorlevel 1 (
    echo.
    echo [PROBLEM] The install did not finish. Check your internet connection and run this file again.
    echo If it keeps failing, take a screenshot of this window.
    echo.
    pause
    exit /b 1
  )
)

REM 2) Make sure the Electron program itself is downloaded
REM    (newer npm versions can skip Electron's own download step).
if not exist "node_modules\electron\dist\electron.exe" (
  echo Downloading Electron, this happens only once...
  node "node_modules\electron\install.js"
)
if not exist "node_modules\electron\dist\electron.exe" (
  echo.
  echo [PROBLEM] Electron could not be downloaded. Check your internet connection and run this file again.
  echo If it keeps failing, delete the node_modules folder next to this file and try again.
  echo.
  pause
  exit /b 1
)

REM 3) Launch the app.
echo Starting AppBlocker... (keep this window open - closing it is fine once the app is in the tray)
"node_modules\electron\dist\electron.exe" .
if errorlevel 1 (
  echo.
  echo [PROBLEM] AppBlocker closed with an error. Take a screenshot of this window.
  echo.
  pause
  exit /b 1
)

echo.
echo AppBlocker closed. You can close this window.
pause
