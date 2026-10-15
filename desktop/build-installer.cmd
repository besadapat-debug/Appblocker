@echo off
setlocal
title AppBlocker - build the installer

REM --- self-elevate to Administrator ---
REM electron-builder extracts a signing helper that contains symlinks, and
REM Windows only lets an ADMIN create symlinks ("a required privilege is not
REM held by the client"). So we run this build elevated.
net session >nul 2>&1
if %errorlevel% neq 0 (
  echo Asking for administrator permission to build...
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

cd /d "%~dp0"

echo ============================================================
echo   Building the AppBlocker Windows installer (one .exe)
echo ============================================================
echo.

REM --- clear any half-extracted electron-builder cache from a failed run ---
if exist "%LocalAppData%\electron-builder\Cache\winCodeSign" (
  echo Clearing a stale build cache from the last attempt...
  rmdir /s /q "%LocalAppData%\electron-builder\Cache\winCodeSign" 2>nul
)

REM --- check Node.js is installed ---
where node >nul 2>&1
if %errorlevel% neq 0 (
  echo  Node.js is not installed. You only need it to BUILD the installer
  echo  once - your end users will NOT need it.
  echo.
  echo  1^) Download the "LTS" installer from:  https://nodejs.org
  echo  2^) Install it ^(just click Next/Finish^).
  echo  3^) Run this file again.
  echo.
  pause
  exit /b
)

echo [1/2] Installing build tools (first time only, may take a few minutes)...
call npm install
if %errorlevel% neq 0 ( echo. & echo  npm install failed - see the messages above. & pause & exit /b )

echo.
echo [2/2] Building the installer...
call npm run dist:win
if %errorlevel% neq 0 ( echo. & echo  Build failed - see the messages above. & pause & exit /b )

echo.
echo ============================================================
echo   DONE. Your installer is in the "dist" folder:
echo       dist\AppBlocker-Setup-^<version^>.exe
echo ============================================================
echo.
echo   That single .exe is what you give people. They double-click
echo   it and AppBlocker installs like any normal Windows app.
echo.
start "" "%~dp0dist"
pause
