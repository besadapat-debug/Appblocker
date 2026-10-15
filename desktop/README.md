# AppBlocker for Windows & macOS

A cross-platform desktop app blocker built with **Electron** (one codebase for
both OSes). Original app — not based on any other blocker's code.

## What it does
- **Block lists** of apps and websites, each with an on/off switch.
- **Website blocking** system-wide via the hosts file (works in every browser).
- **App blocking** by closing blocked programs automatically while they run.
- **Weekly schedules** — block only on chosen days/times (incl. overnight).
- **Strict Mode** — password and/or a wait timer to turn blocking off, keeps
  running in the tray, and blocks quitting/deleting while active.
- **Launch at startup**, runs in the system tray.

## v1.2 — fixes
- **No more PC-wide lag.** The adult filter used to add ~125,000 lines to the
  hosts file, which slowed Windows' DNS lookups so much that Firefox, installers
  and other apps "hung" (Chromium browsers resolve hosts themselves, so they
  seemed fine). The hosts file now only holds your block-list sites (capped at
  3,000 hosts); the old giant section is removed automatically on first run.
- **YouTube (and other big sites) blocked properly.** Each site is expanded to
  its subdomains (www., m., mobile.) and, for popular sites, the extra domains
  they load from (e.g. YouTube: youtu.be, music/kids/studio, youtubei.googleapis.com,
  ytimg.com, ggpht.com). IPv6 entries are added too. See `src/shared/sites.js`.
  If a site was already open, close the tab once; if YouTube still shows a cached
  page, clear its site data (padlock icon > Site settings > Delete data).
- **run.cmd** checks for Node.js, installs everything it needs, downloads
  Electron if npm skipped it, and stays open with a clear message on any error.

## Run it (either OS)
Needs Node.js 18+.
```
npm install
npm start        # run the app (run as Administrator / with sudo for hosts edits)
```

## Build installers
- **Windows** (.exe installer): on Windows, `npm run dist:win` → `dist/`.
- **macOS** (.dmg): must be built **on a Mac**, `npm run dist:mac` → `dist/`.
Cross-building macOS from Windows isn't supported by Apple's tooling, same as
iOS. The Windows build is created on Windows; the Mac build on a Mac.

Put an `icon.ico` (Windows) and `icon.icns` (macOS) in `build/`, plus optional
`tray.png` / `trayTemplate.png`, before packaging.

## Admin rights (important)
Editing the hosts file requires elevation:
- Windows: the installer sets the app to **run as Administrator** (see
  `requestedExecutionLevel` in package.json). Approve the UAC prompt.
- macOS: launch from a terminal with `sudo`, or grant the app rights to write
  `/etc/hosts`. Without elevation, website blocking can't apply (the app shows an
  error bar); app-closing still works.

## App names to block
- Windows: use the executable name, e.g. `Discord.exe`, `Spotify.exe`. The
  "Pick from running apps…" button lists what's currently running.
- macOS: use the process name shown by the picker (e.g. `Discord`).

## Honest limits (same class as any desktop blocker without a driver)
- A user with admin rights can still edit the hosts file by hand or force-quit
  the app. True tamper-proofing needs a background **service/daemon** running as
  SYSTEM/root that re-applies blocks and restarts the app — a larger project I
  can add next if you want (Windows Service + launchd daemon).
- Website blocking uses the hosts file, so it blocks whole domains, not specific
  pages, and doesn't do per-browser rules.
- App blocking closes a program shortly after it opens (a few-second poll); it
  can't stop it from launching in the first instant.

## Privileged helper (tamper-resistant blocking)
The app alone applies blocks while it's running. To make website blocking
**survive the app being closed or the hosts file being hand-edited**, install the
included background helper. It runs elevated, owns the hosts file, and re-applies
the block set every few seconds (schedules) and keeps the adult filter's browser policies in place. Config is shared at
`C:\ProgramData\AppBlocker\config.json` (Windows) or
`/Library/Application Support/AppBlocker/config.json` (macOS).

**Windows** (admin PowerShell, after installing the app):
```
cd "C:\Program Files\AppBlocker\resources\app\src\helper"
PowerShell -ExecutionPolicy Bypass -File .\install-windows.ps1
```
Removes with `uninstall-windows.ps1`. It registers a Scheduled Task that runs as
SYSTEM at boot and restarts on failure.

**macOS** (after moving AppBlocker.app to /Applications):
```
sudo /Applications/AppBlocker.app/Contents/Resources/app/src/helper/install-macos.sh
```
Removes with `uninstall-macos.sh`. It installs a launchd daemon that runs as root
with KeepAlive.

The helper only enforces **website** blocks (the durable part). App-closing runs
in the UI while it's open. The paths in the scripts assume default install
locations — adjust if you installed elsewhere.

## Adult content filter
The **Adult & Web filter** tab has an **Adult content filter** toggle. When on, the
app (and the helper) set browser policies so Chrome, Edge, Brave and Firefox look
up sites through **Cloudflare for Families** DNS (blocks adult + malware sites,
millions of domains, kept up to date) and **Google SafeSearch** is forced on.
Policies are re-applied every minute, and Strict Mode prevents turning it off.

It no longer puts ~62,000 domains in the hosts file: a hosts file that big makes
Windows' DNS Client crawl, so Firefox, installers and other apps hang while
Chromium browsers still work. Uses "automatic" mode, so on a network that blocks
Cloudflare the browser falls back to normal DNS instead of losing the internet.

## Honest limits (unchanged)
- Website blocking is whole-domain via hosts, not per-page or per-browser.
- App-closing is a few-second poll, not an instant launch block.
- With the helper installed, casual tampering (closing the app, editing hosts) is
  reverted within seconds. A user who stops the SYSTEM task / root daemon can
  still override it — full lockdown would need a signed kernel/driver component,
  which is out of scope for a normal app.

## Next steps I can add
- Password lock on the whole app UI, per-app usage stats, import/export lists.

---

## v1.1 — Strict Mode parity with the mobile app

This version brings the desktop app in line with the Android version:

### 1. Per-block Strict Mode (own lock + own unlock method)
Each block list now has its **own** Strict Mode, separate from the big one. Open
a list → "Strict Mode for this list" → set an **Unlock method**, then **Lock this
list**. While locked it can't be turned off, weakened (apps/sites removed or the
schedule loosened) or deleted. The big Strict Mode still locks everything at once.

### 2. Per-lock unlock methods
Every lock — the big Strict Mode, each list, and the adult/web filter — has its
own independent unlock settings:
- **Password**
- **Timer** — wait N minutes after activating
- **Cooldown** — press unlock, then wait out a countdown
- **Follow schedules** — can only be turned off *outside* a weekly schedule you set

Pick none and it turns off instantly; pick several and all must be satisfied.

### 3. The weekly "uninstall window" rule
No schedule — on a block list **or** a Follow-schedules unlock — may cover the
whole week. At least one free minute must remain (e.g. end a day at 23:58).
Saving a 24/7 schedule is rejected. That free minute is the only guaranteed time
to turn things off / uninstall, by design.

### 4. "Follow schedules" never falls open
If "Follow schedules" is chosen but **no** schedule is set, the lock now **stays
locked** instead of unlocking instantly (the mobile bug, fixed here too).

### 5. Adult filter — its own settings + lock
The **Adult & Web filter** tab holds the adult-site filter (family-safe DNS in the browsers) with its own Strict Mode lock and unlock method.

### 6. Web allow-list (block all browsing except allowed) — browser extension
Because the hosts file can only *block* named sites (not "allow only these"),
the allow-list is enforced the **Cold Turkey way**, with two layers:

- **Companion browser extension** (`src/extension/`) for Chrome/Edge. It reads
  your allow-list from the app (localhost control server on 127.0.0.1:47113) and
  blocks every site that isn't allowed. If the app is closed, it keeps enforcing
  the last known list (fail-safe).
- **Browser watch** in the app: while the web filter is on, any browser it can't
  protect (Firefox, Safari, Brave, Opera, …) is closed on sight, and a protected
  browser running **without** the live extension is closed after a short grace
  period. On next launch the **force-install policy** reinstalls the extension —
  so removing it just brings it back, and running without it closes the browser.

**Install the extension**
- *Testing now:* Chrome/Edge → Extensions → Developer mode → **Load unpacked** →
  select `src/extension/`. It connects to the app and enforces immediately.
- *Production (can't-remove):* publish `src/extension/` to the Chrome Web Store
  (it keeps the same ID via the `key` field), or self-host with an update
  manifest. The force-install policy in `src/main/policy.js` already points at
  the Web Store update URL. See `src/extension/EXTENSION_ID.txt`.

Non-browser apps (transport, banking, etc.) are never touched by the web filter —
only browsers are.

### Honest limits (unchanged philosophy)
- Editing the hosts file needs admin/root — run the app elevated (the installer
  sets this on Windows).
- There is **no uninstall feature in the app** — the weekly free minute is the
  escape hatch.
- The web allow-list can still be bypassed by a browser forcibly started with
  policies/extensions stripped, a portable browser, or a VPN/DoH tunnel, until
  the watch closes it on the next tick. A determined admin user can always edit
  the config/hosts by hand — this raises friction, it isn't unbreakable.

---

## v1.2 — reliable website blocking + browser enforcement

**Why Instagram still loaded before:** modern browsers (Chrome, Brave, Edge, Firefox)
use encrypted DNS (DoH), which bypasses the Windows hosts file. So a hosts-file block
of instagram.com does nothing in those browsers. Fixed by blocking sites **inside the
browser** with the companion extension — the Cold Turkey approach.

- **Block-list websites are now enforced by the extension** (DoH-proof), in addition to
  the hosts file. You MUST install the extension for website blocking to be reliable:
  Settings tab shows a green "Extension connected" line and tells you how (Load unpacked
  → `src\extension`). A yellow banner appears on the Blocking tab if it's missing.
- **Brave/Vivaldi/Opera are now "protected"** (they can run the extension), so your
  everyday Chromium browser isn't force-closed. Only browsers that can't run the
  extension (Firefox, Safari, Tor) are closed while web blocking is active.
- **"Close browsers that can bypass blocking"** (Settings, on by default) extends the
  Cold-Turkey close-on-bypass to block lists too, not just the allow-list. Can't be
  turned off while the big Strict Mode is on.

### The weekly free-minute rule (unchanged, reaffirmed)
Every schedule — block lists and Follow-schedules unlocks — must leave **at least one
free minute per week**. A 24/7 schedule is rejected on save. That minute is the only
guaranteed window to turn things off, because there is no uninstaller.

### Blocking Safe Mode / Reset / uninstall — what's actually possible (honest)
A user-space app genuinely **cannot** fully block Windows Safe Mode, "Reset this PC", or
a determined admin deleting its files — and trying (editing boot config / BCD) can leave
a PC unbootable, which is unacceptable when there's no uninstaller. What this app does
instead, safely:
- No uninstaller is shipped.
- While the big Strict Mode is on: quitting and closing are blocked, the browser policies
  and DNS settings are re-applied every minute (so deleting the registry keys only lasts
  until the next check), and "Close browsers that can bypass" can't be switched off.
- For blocking that survives even a reboot into Safe Mode, the privileged **helper
  service** (install-windows.ps1) must be installed — a scheduled task/service running as
  SYSTEM. Even then, Safe Mode and Reset can't be *prevented*, only made harder to profit
  from. This is the same ceiling every blocker (including Cold Turkey) runs into without a
  signed kernel driver.

---

## v1.3 — the extension installs itself (no manual steps)

The companion extension is now **force-installed automatically** in Chrome, Edge and
Brave — no "Load unpacked":

- The app serves a signed `.crx` (`src/extension/appblocker.crx`) and an update manifest
  from its own local server, and sets each browser's force-install policy to point at it.
  On browser launch the extension installs itself, **can't be removed** (removing a
  force-installed extension is blocked), and **reinstalls on next launch** if the policy
  is cleared. The app re-asserts the policy every 10s.
- "Close browsers that can bypass blocking" + the extension = the full Cold-Turkey loop:
  a browser without the extension is closed; when it reopens, the extension auto-installs,
  heartbeats, and the browser is allowed. Browsers that can't run it (Firefox, Safari, Tor)
  are closed whenever web blocking is active.
- Signing key: the `.crx` is signed with a key generated for this build so its ID stays
  fixed. To remove the extension entirely (e.g. to uninstall the whole app), delete the
  `ExtensionInstallForcelist` values under `HKCU\Software\Policies\{Google\Chrome,
  Microsoft\Edge, BraveSoftware\Brave}` and close the app.
- If a particular browser build refuses the local update URL, the manual Load-unpacked
  fallback (Settings tab) still works, and browserwatch still enforces either way.

---

## v1.4 — store-based auto-install (Chrome/Edge/Brave + Firefox)

Silent, user-free extension install is done the only way browsers allow it: via the
official stores. Two ready-to-upload packages are in `dist-store/`
(`appblocker-chrome.zip`, `appblocker-firefox.zip`), and **PUBLISHING.md** has the
step-by-step. After publishing, the app's force-install policies install the extension
automatically and unremovably on Chrome, Edge, Brave and Firefox — no "Load unpacked",
nothing for the user to do. Until you publish, Load unpacked (Settings tab) is the test path.
