; Custom NSIS hooks for AppBlocker (electron-builder).
; Runs during the (elevated, per-machine) install/uninstall.

!macro customInstall
  ; Point the helper launcher at the installed app with absolute paths.
  FileOpen $0 "$INSTDIR\resources\app\src\helper\run-helper.cmd" w
  FileWrite $0 "@echo off$\r$\n"
  FileWrite $0 "set ELECTRON_RUN_AS_NODE=1$\r$\n"
  FileWrite $0 '"$INSTDIR\AppBlocker.exe" "$INSTDIR\resources\app\src\helper\helper.js"$\r$\n'
  FileClose $0

  ; Register + start the SYSTEM helper (hosts/DNS enforcement + Safe-Mode lock +
  ; file protection). It restarts itself if killed. WinRE/boot are left intact.
  nsExec::Exec 'powershell -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\resources\app\src\helper\install-windows.ps1"'
!macroend

!macro customUnInstall
  ; Refuse to uninstall while any lock is active - removal is only allowed in the
  ; weekly buffer window (when Strict Mode is off).
  nsExec::Exec 'powershell -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\resources\app\src\helper\is-locked.ps1"'
  Pop $0
  StrCmp $0 "1" 0 +4
    MessageBox MB_OK "AppBlocker is in Strict Mode and can't be uninstalled right now. It can be removed during your weekly buffer window, when Strict Mode turns off."
    Abort
    Goto done
  done:
  ; Remove the SYSTEM helper task and release every protection it applied.
  nsExec::Exec 'powershell -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\resources\app\src\helper\uninstall-windows.ps1"'
!macroend
