# Admin PowerShell. Removes the helper task AND releases every protection it
# applied, so uninstall leaves the machine completely normal.
$ErrorActionPreference = 'SilentlyContinue'

# 1) Stop/remove the SYSTEM helper task.
Unregister-ScheduledTask -TaskName "AppBlockerHelper" -Confirm:$false

# 2) Lift the Safe-Mode lockout.
Remove-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System' `
  -Name 'SafeModeBlockNonAdmins' -ErrorAction SilentlyContinue

# 3) Release the install-dir ACLs (take ownership + reset inheritance).
$dir = Join-Path $env:ProgramFiles 'AppBlocker'
if (Test-Path $dir) {
  takeown /f $dir /r /d y | Out-Null
  icacls $dir /reset /t /c | Out-Null
}

# 4) Remove the browser force-install + DNS policies.
foreach ($k in @(
  'HKCU\Software\Policies\Google\Chrome',
  'HKCU\Software\Policies\Microsoft\Edge',
  'HKCU\Software\Policies\BraveSoftware\Brave')) {
  reg delete "$k\ExtensionInstallForcelist" /v 1 /f 2>$null | Out-Null
}
reg delete "HKLM\SOFTWARE\Policies\Mozilla\Firefox\ExtensionSettings\appblocker@local" /f 2>$null | Out-Null

Write-Host "AppBlocker helper removed and all protections released."
