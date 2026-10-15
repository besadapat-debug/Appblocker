@echo off
setlocal enabledelayedexpansion
REM ============================================================================
REM  AppBlocker - DEV helper: fully remove the force-installed (policy) extension
REM  so you can Load unpacked the local src\extension for testing.
REM
REM  Double-click it, click "Yes" for admin. It stops the AppBlocker app (so it
REM  can't re-add the policy), optionally closes your browsers, clears the
REM  force-install everywhere, then VERIFIES it's gone.
REM
REM  DEVELOPER TOOL - do not ship to end users.
REM ============================================================================

REM --- self-elevate to Administrator ---
net session >nul 2>&1
if %errorlevel% neq 0 (
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

echo.
echo ============================================================
echo    AppBlocker - remove force-installed extension (DEV)
echo ============================================================
echo.

echo [1/4] Stopping the AppBlocker app so it can't re-add the policy...
taskkill /IM AppBlocker.exe /F /T >nul 2>&1
taskkill /IM electron.exe  /F /T >nul 2>&1
timeout /t 1 >nul

echo.
echo    Close your browsers too? They must be fully closed for the removal to
echo    show. Choosing Y will force-close Chrome, Brave and Edge (you'll lose
echo    unsaved tabs). Choose N to close them yourself afterwards.
choice /c YN /m "   Force-close browsers now"
if errorlevel 2 goto skipbrowsers
echo [2/4] Closing browsers...
taskkill /IM chrome.exe /F /T >nul 2>&1
taskkill /IM brave.exe  /F /T >nul 2>&1
taskkill /IM msedge.exe /F /T >nul 2>&1
taskkill /IM vivaldi.exe /F /T >nul 2>&1
taskkill /IM opera.exe  /F /T >nul 2>&1
goto afterbrowsers
:skipbrowsers
echo [2/4] Leaving browsers open (close them yourself after this finishes).
:afterbrowsers

echo [3/4] Removing the force-install policies...
for %%K in (
  "HKCU\Software\Policies\Google\Chrome\ExtensionInstallForcelist"
  "HKCU\Software\Policies\Microsoft\Edge\ExtensionInstallForcelist"
  "HKCU\Software\Policies\BraveSoftware\Brave\ExtensionInstallForcelist"
  "HKCU\Software\Policies\Vivaldi\ExtensionInstallForcelist"
  "HKCU\Software\Policies\Opera Software\Opera Stable\ExtensionInstallForcelist"
) do (
  reg delete %%K /f >nul 2>&1
)
reg delete "HKLM\SOFTWARE\Policies\Mozilla\Firefox\ExtensionSettings\appblocker@local" /f >nul 2>&1

echo [4/4] Verifying...
set STILL=0
for %%K in (
  "HKCU\Software\Policies\Google\Chrome\ExtensionInstallForcelist"
  "HKCU\Software\Policies\Microsoft\Edge\ExtensionInstallForcelist"
  "HKCU\Software\Policies\BraveSoftware\Brave\ExtensionInstallForcelist"
) do (
  reg query %%K >nul 2>&1 && set STILL=1
)

echo.
if "!STILL!"=="1" (
  echo   X  The policy came BACK - the AppBlocker app is still running and
  echo      re-adding it. Quit AppBlocker from the system tray ^(right-click
  echo      its icon near the clock - Quit^), check Task Manager for any
  echo      "AppBlocker" or "electron" process, then run this again.
) else (
  echo   OK  Force-install removed. Reopen your browser - the extension is gone.
  echo.
  echo   Test the LATEST local extension:
  echo     Chrome/Brave  brave://extensions - Developer mode - Load unpacked
  echo                   folder:  %~dp0src\extension
  echo     Firefox       about:debugging - Load Temporary Add-on
  echo                   file:    %~dp0src\extension-firefox\manifest.json
  echo.
  echo   Note: reopening the AppBlocker app force-installs the STORE copy again.
)
echo.
pause
