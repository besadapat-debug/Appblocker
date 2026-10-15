# AppBlocker self-defense (Windows)

Makes the app resist casual bypass while Strict Mode is active, **without**
removing the machine's ability to recover.

## What it does (only while something is locked)

Driven by the SYSTEM helper (`src/helper/selfdefense.js`), on each tick:

- **Safe-Mode lockout** — sets `SafeModeBlockNonAdmins = 1` so a **Standard user**
  can't boot into Safe Mode to kill the helper. (`scripts/safemode.ps1`)
- **Install-dir ACLs** — removes write/delete from Users and Administrators on
  `C:\Program Files\AppBlocker`; SYSTEM keeps full control. (`scripts/acl.ps1`)
- **Resilient service (optional)** — `scripts/install-service.ps1` installs the
  helper as a SYSTEM service that auto-restarts if force-killed.

## The safety design (why this is OK to ship)

Every protection is released the moment nothing is locked — i.e. during your
**weekly buffer window**, when the user turns Strict Mode off. On that tick the
helper runs `safemode.ps1 disable` and `acl.ps1 unlock` (take-ownership + reset),
so the app is removable and the machine is fully normal again.

Three guaranteed recovery paths always remain:

1. The **weekly buffer** (automatic, at most one week away).
2. An **Administrator** can take ownership and reset the ACLs / clear the policy
   at any time — the locks are friction, not a wall.
3. The **Windows Recovery Environment is left completely intact.** We do not run
   `reagentc`, do not touch boot config, and do not fight Windows Update. If the
   PC ever fails to boot, standard Windows recovery still works.

The helper **fails open**: any error leaves the machine recoverable rather than
locking into an un-undoable state.

## What it deliberately does NOT do

- It does **not** disable WinRE / Shift-Restart / "Reset this PC".
- It does **not** try to be unremovable by an Administrator.

Disabling WinRE would remove recovery exactly when it's needed (a boot failure
during a locked week never reaches the buffer to undo it) and is the anti-recovery
behaviour malware uses. The Safe-Mode lockout already stops the realistic bypass.

## Requirements / notes

- Active only when the **SYSTEM helper** is installed (`src/helper/install-windows.ps1`),
  not in dev runs from a folder. ACLs apply only if installed to `C:\Program Files\AppBlocker`.
- For the Safe-Mode lockout to bite, the person must use a **Standard user**
  account for daily use, with a separate Administrator account.
- **Disclose it.** Your EULA / first-run screen should state plainly that the app
  (a) blocks Safe Mode for standard users and (b) protects its own files while
  Strict Mode is active, and that the weekly buffer is when it can be removed.
  That disclosure is what keeps it classified as a focus tool, not unwanted software.

## Optional hardening: BitLocker (the right way to lock down the recovery shell)

The one residual bypass is a user booting Advanced Startup → command prompt and
deleting the app's files. The safe way to raise that bar is **BitLocker**, not
disabling WinRE: with the system drive encrypted, a recovery command prompt needs
the 48-digit recovery key, while Windows recovery itself stays fully intact.

The app only **detects and guides** (Settings → "Drive encryption"):
- `src/main/bitlocker.js` reports whether BitLocker is ON for the system drive.
- If OFF, the Settings card recommends it and opens **Windows' own BitLocker
  wizard** (`control /name Microsoft.BitLockerDriveEncryption`).

We deliberately do **not** silently run `Enable-BitLocker`. Encryption is
irreversible-if-the-key-is-lost; key backup (to a Microsoft account, a file, or
print) must happen through Windows' UI with the user's eyes open. Forcing it from
code risks permanently locking a user out of their own data — the same class of
harm as disabling recovery, just from the opposite direction.
