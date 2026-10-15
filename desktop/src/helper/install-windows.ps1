# Run this in an ADMIN PowerShell after installing AppBlocker.
# It registers a Scheduled Task that runs the privileged helper at boot as
# SYSTEM and keeps it running, so website blocking survives the app being closed
# or the hosts file being hand-edited.
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$cmd  = Join-Path $here "run-helper.cmd"
if (!(Test-Path $cmd)) { throw "run-helper.cmd not found next to this script." }

$action   = New-ScheduledTaskAction -Execute $cmd
$trigger  = New-ScheduledTaskTrigger -AtStartup
$principal= New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero)

Register-ScheduledTask -TaskName "AppBlockerHelper" -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Force
Start-ScheduledTask -TaskName "AppBlockerHelper"
Write-Host "AppBlocker helper installed and started."
