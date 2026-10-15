@echo off
REM ============================================================================
REM  AppBlocker - full DEV cleanup. Undoes everything the dev copy changed on
REM  this PC, so you can safely delete the folder. Double-click, click "Yes".
REM
REM  IMPORTANT: turn OFF Strict Mode and any locks in the app FIRST. If a lock
REM  is on, blocking is meant to stay - clear it in the app before running this.
REM ============================================================================

REM --- self-elevate to Administrator ---
net session >nul 2>&1
if %errorlevel% neq 0 (
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

echo.
echo ============================================================
echo    AppBlocker - removing everything from this PC
echo ============================================================
echo.

echo [1/6] Closing the AppBlocker app...
taskkill /IM AppBlocker.exe /F /T >nul 2>&1
taskkill /IM electron.exe  /F /T >nul 2>&1

echo [2/6] Removing the background helper task (if present)...
schtasks /End    /TN "AppBlockerHelper" >nul 2>&1
schtasks /Delete /TN "AppBlockerHelper" /F >nul 2>&1

echo [3/6] Lifting the Safe-Mode block (if set)...
reg delete "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System" /v SafeModeBlockNonAdmins /f >nul 2>&1

echo [4/6] Removing the browser extension force-install policies...
for %%K in (
  "HKCU\Software\Policies\Google\Chrome\ExtensionInstallForcelist"
  "HKCU\Software\Policies\Microsoft\Edge\ExtensionInstallForcelist"
  "HKCU\Software\Policies\BraveSoftware\Brave\ExtensionInstallForcelist"
  "HKCU\Software\Policies\Vivaldi\ExtensionInstallForcelist"
) do reg delete %%K /f >nul 2>&1
reg delete "HKLM\SOFTWARE\Policies\Mozilla\Firefox\ExtensionSettings\appblocker@local" /f >nul 2>&1

echo [5/6] Cleaning AppBlocker's lines out of the hosts file...
powershell -NoProfile -Command ^
  "$h=\"$env:windir\System32\drivers\etc\hosts\"; if(Test-Path $h){ $t=Get-Content $h -Raw; $b=$t.IndexOf('# >>> AppBlocker begin >>>'); if($b -ge 0){ $m='# <<< AppBlocker end <<<'; $e=$t.IndexOf($m,$b); if($e -ge 0){ $e+=$m.Length } else { $e=$t.Length }; $t=$t.Substring(0,$b)+$t.Substring($e); Set-Content $h $t -NoNewline -Encoding ascii } }" 2>nul

echo [6/6] Restoring normal DNS / SafeSearch policies (if set)...
for %%K in (
  "HKCU\Software\Policies\Google\Chrome\DnsOverHttpsMode"
  "HKCU\Software\Policies\Google\Chrome\DnsOverHttpsTemplates"
  "HKCU\Software\Policies\Google\Chrome\ForceGoogleSafeSearch"
  "HKCU\Software\Policies\BraveSoftware\Brave\DnsOverHttpsMode"
  "HKCU\Software\Policies\BraveSoftware\Brave\ForceGoogleSafeSearch"
  "HKCU\Software\Policies\Microsoft\Edge\DnsOverHttpsMode"
  "HKCU\Software\Policies\Microsoft\Edge\ForceGoogleSafeSearch"
) do reg delete %%~K /f >nul 2>&1

echo.
echo ============================================================
echo   DONE - this PC is back to normal.
echo.
echo   Now you can delete the whole AppBlockerDesktop folder.
echo   (If Windows says a file is in use, close the app from the
echo    system tray first, then delete.)
echo.
echo   Restart your browsers so they drop the extension.
echo ============================================================
echo.
pause
