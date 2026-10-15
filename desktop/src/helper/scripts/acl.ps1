# Lock/unlock the install directory. "lock" removes write/delete for Users and
# Administrators (SYSTEM keeps full control). "unlock" takes ownership back and
# resets inheritance - the guaranteed reversal path, run during the buffer.
param([Parameter(Mandatory=$true)][ValidateSet('lock','unlock')][string]$mode,
      [Parameter(Mandatory=$true)][string]$dir)
$ErrorActionPreference = 'SilentlyContinue'
. (Join-Path $PSScriptRoot '_admin.ps1'); Assert-Admin
if (-not (Test-Path $dir)) { exit 0 }   # not installed to Program Files (e.g. dev) -> nothing to do

if ($mode -eq 'lock') {
  icacls $dir /inheritance:r                         | Out-Null
  icacls $dir /grant:r "*S-1-5-18:(OI)(CI)F"         | Out-Null   # SYSTEM: full
  icacls $dir /grant:r "*S-1-5-32-544:(OI)(CI)RX"    | Out-Null   # Administrators: read/exec
  icacls $dir /grant:r "*S-1-5-32-545:(OI)(CI)RX"    | Out-Null   # Users: read/exec
} else {
  takeown /f $dir /r /d y                            | Out-Null
  icacls $dir /reset /t /c                           | Out-Null
}
exit 0
