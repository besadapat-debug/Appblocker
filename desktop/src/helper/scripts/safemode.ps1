# Enable/disable the Safe Mode lockout for NON-admin users. Reversible; an admin
# account can always still reach Safe Mode, which is the intended escape hatch.
param([Parameter(Mandatory=$true)][ValidateSet('enable','disable')][string]$mode)
$ErrorActionPreference = 'SilentlyContinue'
. (Join-Path $PSScriptRoot '_admin.ps1'); Assert-Admin

$sys = 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System'
if ($mode -eq 'enable') {
  New-Item -Path $sys -Force | Out-Null
  New-ItemProperty -Path $sys -Name 'SafeModeBlockNonAdmins' -Value 1 -PropertyType DWord -Force | Out-Null
} else {
  Remove-ItemProperty -Path $sys -Name 'SafeModeBlockNonAdmins' -ErrorAction SilentlyContinue
}
exit 0
