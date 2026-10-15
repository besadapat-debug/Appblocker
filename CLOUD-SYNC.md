# Account sign-in + multi-device sync (new)

Your existing AppBlocker is unchanged — every feature still works fully
**on-device with no account**. Signing in only *adds* sync. It uses the **same
Supabase account as the desktop app**.

## What it adds
- **Account tab:** it's in **Profile → Account & sync** (top card). Tap it to
  **Sign in** or **Create account** (same email/password as the desktop app).
- **Block-list sync:** whenever you add, edit, enable/disable or delete a block
  list, it's pushed to the cloud and pulled onto your other signed-in devices
  automatically (live, via Supabase realtime).
- **Remote control:** under **Your devices**, every other device shows
  **Strict on** and **Lock all** buttons. "Strict on" arms Strict Mode on that
  device; "Lock all" turns every block list on *and* arms Strict Mode.
- **Locked devices stay locked:** while a device is in Strict Mode it ignores
  any remote change that would weaken it.

## How it's built (for reference)
- `cloud/CloudSync.kt` — sign in/up/out, device registration, push/pull of the
  block-lists JSON blob, and realtime subscriptions for configs + commands.
- `cloud/CloudModels.kt` — the `configs` / `devices` / `commands` row shapes.
- `AppBlockerApp.kt` — initialises the cloud layer once at process start.
- `BlockListStore` now pushes on every save and can apply a remote list set.
- Only devices running **this** app sync with each other (configs are tagged
  `source: "android"`), so a differently-shaped config can't overwrite yours.

## Toolchain
Bumped to Kotlin 2.0.20 / AGP 8.6.1 / Gradle 8.9 (needed by supabase-kt 3.0.3 +
ktor 3). The GitHub Actions workflow uses Gradle 8.9 and Java 17. Nothing else
about the build changed — same `com.example.appblocker` id, same debug keystore,
same `assembleDebug` → `AppBlocker.apk` release flow.

## Note on sharing one account with the desktop app
All your devices share one cloud config row per account, so the most recent
edit wins. If you run the desktop app and this app on the same account and want
them to sync block lists too (not just phones), tell me and I'll align the two
config formats.
