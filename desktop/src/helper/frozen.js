// "Frozen Turkey" enforcement (runs in the SYSTEM helper). During a freeze
// schedule, the named Windows account is signed out on sight - except during the
// weekly unlock window, which always lifts the freeze so there's a way out.
const { execFile } = require('child_process');
const { rangeActive } = require('../shared/schedule');
const { withinWeeklyWindow } = require('../shared/weekly');

let last = 0;

function reconcile(cfg) {
  if (process.platform !== 'win32') return;
  const now = Date.now();
  if (now - last < 4000) return; // throttle
  last = now;
  try {
    const accts = cfg.frozenAccounts || [];
    if (!accts.length) return;
    // The weekly window always lifts the freeze (the one time out).
    if (withinWeeklyWindow(cfg.weeklyWindow)) return;

    for (const a of accts) {
      if (!a || a.enabled === false || !a.username) continue;
      const active = (a.ranges || []).some(r => rangeActive(r));
      if (!active) continue;
      logoffUser(a.username);
    }
  } catch (e) { /* fail open */ }
}

// Find the named user's active session and log it off. SYSTEM can do this.
function logoffUser(username) {
  const ps =
    "$name='" + username.replace(/'/g, "''") + "';" +
    "$lines = (quser 2>$null);" +
    "foreach($l in $lines){" +
    "  $t=$l.Trim();" +
    "  if($t -match ('(?i)^>?\\s*'+[regex]::Escape($name)+'\\s')){" +
    "    $cols=($t -replace '\\s{2,}',',').Split(',');" +
    "    $id=$null;" +
    "    foreach($c in $cols){ if($c -match '^[0-9]+$'){ $id=$c; break } }" +
    "    if($id){ logoff $id }" +
    "  }" +
    "}";
  execFile('powershell', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', ps], () => {});
}

module.exports = { reconcile };
