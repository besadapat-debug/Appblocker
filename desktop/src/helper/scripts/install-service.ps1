# OPTIONAL: install the helper as a resilient SYSTEM service that auto-restarts
# if it is force-killed. Run elevated. Point $bin at your installed helper exe.
# (Leaves WinRE and boot config untouched.)
param([string]$bin = "C:\Program Files\AppBlocker\helper.exe",
      [string]$svc = "AppBlockerHelper")
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_admin.ps1'); Assert-Admin

sc.exe create $svc binPath= "`"$bin`"" start= auto obj= LocalSystem DisplayName= "AppBlocker Helper" | Out-Null
# 1st/2nd/subsequent failure -> restart after 5s; never reset the failure counter.
sc.exe failure $svc reset= 0 actions= restart/5000/restart/5000/restart/5000 | Out-Null
# Treat a non-zero clean exit as a failure too, so a graceful kill still restarts.
sc.exe failureflag $svc 1 | Out-Null
sc.exe start $svc | Out-Null
Write-Host "AppBlocker Helper service installed with auto-restart."
