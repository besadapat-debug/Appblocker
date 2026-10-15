# Per-item Strict locks (new)

Previously there was one global Strict Mode, so unlocking it to use (say)
social media also unlocked everything else, including adult sites. Now **each
block list, and the adult filter, has its own independent Strict lock**, and the
app can only be uninstalled when **every** lock is off.

## How it works
- **Per-list lock:** every block list in the Blocking tab now has a padlock
  button (🔓 / 🔒). Tap it to open that list's own lock screen, choose its
  unlock method (Timer / Follow schedules / Charger / Cooldown / PIN — same
  engine as the master lock), and activate it. Each list's method, timer, PIN
  and countdown are stored separately.
- **Independent:** unlocking the "Social media" list's lock does **not** unlock
  the "Adult sites" list. They each hold until their own unlock condition is met.
  So a 5-minute timer on social media can't be used to reach adult sites that
  you locked with, say, a PIN you gave to someone else.
- **Adult filter lock:** the adult content filter has its own padlock too, so it
  can be locked on completely separately.
- **While a list is locked:** it can't be disabled, deleted, or weakened (the
  editor opens add-only), locally or from the cloud.

## Uninstall / tamper protection = ALL locks
The app's self-defence (blocking its own uninstall, App info, accessibility
toggle, device-admin screen, factory reset, Recent apps, and Settings-in-split)
now stays on while **any** lock is active — the master Strict lock **or** any
per-list lock **or** the adult lock. Advanced options (block uninstall / block
settings / block recents) also can't be switched off while any lock is on.

**Result:** to uninstall the app, you must deactivate every per-list lock, the
adult lock, and the master Strict lock. As long as one lock is still on, the app
can't be removed and its protective settings can't be turned off.

## Where things are
- Master lock + advanced options: the **Strict** tab (unchanged).
- Per-list locks: the 🔓 button on each list in the **Blocking** tab.
- Adult filter lock: the 🔓 button next to the Adult content filter.

## Technical notes
- `StrictModeStore`, `UnlockMethodStore`, `StrictScheduleStore` and
  `UnlockChecker` are now *scope-aware* (`""` = master, `"_list_<id>"` per list,
  `"_adult_"` for the adult filter). Existing master behaviour is unchanged.
- `ListLocks` enumerates the locks and exposes `anyLockActive()`, which now
  gates the tamper/uninstall/recents protection in `AppBlockAccessibilityService`
  and the Advanced-options screen.
- `ListLockActivity` is the per-item lock screen; it reuses the exact same
  activate/deactivate flow as the master Strict tab, scoped to one item.
