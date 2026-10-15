# Publishing the AppBlocker browser extension

Publishing the extension once makes it **install itself silently** on your users'
machines (via the force-install policy the app already sets) and makes it
**unremovable** — on Chrome, Edge, Brave and Firefox. No "Load unpacked", no user
action. This is the same approach Cold Turkey and Freedom use.

You upload two packages (both are in the `dist-store/` folder):

- `dist-store/appblocker-chrome.zip`  → Chrome Web Store (covers Chrome + Edge + Brave)
- `dist-store/appblocker-firefox.zip` → Firefox Add-ons (AMO)

---

## 1. Chrome Web Store (Chrome, Edge, Brave)

1. Go to the Chrome Web Store Developer Dashboard:
   https://chrome.google.com/webstore/devconsole
2. Pay the one-time **US$5** developer registration fee (once per account, ever).
3. Click **Add new item** → upload `dist-store/appblocker-chrome.zip`.
4. Fill in the listing:
   - **Name:** AppBlocker Web Filter
   - **Description:** "Companion extension for the AppBlocker app. Enforces the
     website block lists and allow-list you set in AppBlocker."
   - **Category:** Productivity
   - **Single purpose:** "Block websites according to the user's AppBlocker settings."
   - **Permission justifications:**
     - `webNavigation` / `tabs` – to detect and redirect blocked page loads.
     - host access (`<all_urls>`) – blocking can apply to any site the user lists.
     - `storage` – cache the current block list so it works if the app is closed.
     - `127.0.0.1` – read the block list from the local AppBlocker app.
   - Add an icon (128×128) and at least one screenshot (the blocked-page screen is fine).
5. Submit for review. Approval is usually a few days.
6. **After approval, copy the extension ID** shown on the dashboard (a 32-letter string).
   The Web Store assigns its own ID — it will NOT be the local-testing one — so open
   `src/main/policy.js` and set:
   ```js
   const EXT_ID = '<the id from the Web Store>';
   ```
   (The store rejects a manifest that contains a `key` field, which is why the uploaded
   package doesn't have one. The local `src/extension` keeps its `key` only for
   load-unpacked testing; that's expected and fine.)

After that, the app's force-install policy (`src/main/policy.js`, already pointed at the
Web Store) auto-installs it in Chrome, Edge and Brave.

---

## 2. Firefox Add-ons (AMO)

1. Go to https://addons.mozilla.org/developers/ and sign in (free, no fee).
2. **Submit a New Add-on** → **On this site** → upload `dist-store/appblocker-firefox.zip`.
3. Choose distribution **"On this site"** (listed) so you get an auto-updating URL.
4. Fill in the listing (same text as above) and submit. Review is usually quick.
5. **After approval, copy the add-on's download URL.** It looks like:
   `https://addons.mozilla.org/firefox/downloads/latest/<your-slug>/latest.xpi`
6. Open `src/main/policy.js` and paste it into `FF_XPI_URL`:
   ```js
   const FF_XPI_URL = 'https://addons.mozilla.org/firefox/downloads/latest/<your-slug>/latest.xpi';
   ```
   (Keep `FF_ID = 'appblocker@local'` — it matches the Firefox manifest.)

Now the app force-installs it in Firefox too (via Firefox's enterprise policy,
written to `HKLM\SOFTWARE\Policies\Mozilla\Firefox\ExtensionSettings`). Firefox must
be allowed to read enterprise policies, which it does by default on Windows.

> macOS Firefox: instead of the registry, drop a `policies.json` in Firefox's
> `distribution` folder with an `ExtensionSettings` entry using the same id and URL.

---

## While you wait for approval (testing now)

The extension still works immediately via **Load unpacked** (Chrome/Brave:
`chrome://extensions` → Developer mode → Load unpacked → `src/extension`; Firefox:
`about:debugging` → This Firefox → Load Temporary Add-on → pick
`src/extension-firefox/manifest.json`). Blocking and the browser-close enforcement
work the same either way — publishing just removes the manual step for your users.

## Keeping the ID stable across updates

The Chrome package keeps its `key` field so its ID never changes. When you update the
extension later, bump `version` in the manifest and re-upload the new zip to each store.
