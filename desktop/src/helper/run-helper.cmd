@echo off
REM Launches the AppBlocker helper using the bundled Electron as a Node runtime.
set ELECTRON_RUN_AS_NODE=1
"%~dp0..\..\AppBlocker.exe" "%~dp0helper.js"
