# AppBlocker cloud sync + remote control (Supabase)

This adds: **one login across devices**, **blocks that sync**, and **arming Strict
Mode on one device from another** (phone ↔ laptop, both directions).

The app works 100% locally without any of this — cloud is optional.

## 1. Create a Supabase project
1. Go to https://supabase.com → **New project** (free tier is fine).
2. Once it's ready, open **Project Settings → API** and copy:
   - **Project URL** (e.g. `https://abcd.supabase.co`)
   - **anon public** key (starts with `eyJ...`)

## 2. Create the database
- Open **SQL Editor → New query**, paste the contents of **`supabase/schema.sql`**,
  and click **Run**. That creates the `configs`, `devices` and `commands` tables,
  locks them down with row-level security (each user sees only their own data),
  and turns on realtime.

## 3. Connect the app
Two options:
- **Ship it preconfigured (recommended for your users):** open `src/main/sync.js`
  and paste your values into `DEFAULT_URL` and `DEFAULT_ANON`. Rebuild. Your users
  then just sign in — no setup on their side. (Both values are public client keys,
  safe to embed.)
- **Per-machine:** in the app → **Settings → Account & sync**, paste the URL and
  anon key, Save, and restart.

## 4. Use it
1. Install the Node dependency once (first build): run `npm install` (the
   `build-installer.cmd` already does this).
2. Open **Settings → Account & sync → Create account / Sign in**.
3. Sign in with the **same account** on your phone and other computers.
4. Your blocks appear everywhere automatically. Arm Strict Mode per device, or use
   the **Strict on / Lock all** buttons next to another device to control it remotely.

## How the pieces map (for the mobile apps)
Your Android (Kotlin) and iOS (SwiftUI) apps talk to the **same three tables**:
- **`configs`** — one row per user, `data` = the blocks JSON. Pull on login, upsert on change, subscribe for realtime updates.
- **`devices`** — insert a row per device on first login; update `last_seen`.
- **`commands`** — insert `{type:'strict_on'|'lock_all'|'strict_off', target_device}` to control another device; subscribe to act on incoming ones.

Use the Supabase Kotlin and Swift client libraries with the same URL + anon key.
Remote `strict_off` must still run through the device's own unlock check — never a
hard override — exactly like the desktop app does.

## Safety notes
- A device that is **currently locked ignores remote config changes** (so a remote
  edit can't weaken a locked device).
- Remote **strict_off** asks the device to unlock; it still obeys that device's
  password/timer/cooldown/schedule. It cannot bypass a real lock.
- Row-level security means users can only ever read/write their **own** rows.
