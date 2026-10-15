# Exit 1 if any AppBlocker lock is currently ON (Strict Mode / a list / the
# filter), else exit 0. Used by the uninstaller to refuse removal while locked.
$ErrorActionPreference = 'SilentlyContinue'
try {
  $cfg = Join-Path $env:ProgramData 'AppBlocker\config.json'
  $j = Get-Content $cfg -Raw | ConvertFrom-Json
  $on = $false
  if ($j.strict.enabled) { $on = $true }
  if ($j.adultStrict.enabled) { $on = $true }
  foreach ($l in $j.lists) { if ($l.strict.enabled) { $on = $true } }
  if ($on) { exit 1 } else { exit 0 }
} catch { exit 0 }
