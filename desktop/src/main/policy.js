// Writes the per-user browser policy that FORCE-INSTALLS the companion
// extension into Chrome and Edge, so the user can't remove it (removing a
// force-installed extension is blocked, and it's reinstalled on launch).
//
// Per-user policy (HKCU on Windows, `defaults` on macOS) needs no admin.
//
// NOTE: force-install by ID only takes effect once the extension is published
// with this exact ID - either to the Chrome Web Store, or self-hosted with an
// update manifest. Until then, load it unpacked for testing; browserwatch still
// enforces the allow-list regardless of how it's installed. See README and
// src/extension/EXTENSION_ID.txt.
const { execFile } = require('child_process');

// The Chromium extension ID. Keeping the "key" field in manifest.json pins this
// ID through a Chrome Web Store upload, so this stays valid after publishing.
// If the store ever shows a different ID, paste it here.
const EXT_ID = 'ikmgldgfiabicemfehgekepejmgdblkj';
// Chrome Web Store update endpoint. Force-install takes effect once the
// extension is PUBLISHED to the Web Store (Chrome, Edge and Brave all honour
// this). Before publishing, load it unpacked for testing. See PUBLISHING.md.
const UPDATE_URL = 'https://clients2.google.com/service/update2/crx';
const FORCE_VALUE = `${EXT_ID};${UPDATE_URL}`;

// Firefox: force-install via its own policy engine. The install URL is only
// known AFTER the add-on is approved on addons.mozilla.org (AMO). Paste it here
// (e.g. https://addons.mozilla.org/firefox/downloads/latest/<slug>/latest.xpi);
// while it's empty, Firefox force-install is skipped. Must match the gecko id in
// src/extension-firefox/manifest.json. See PUBLISHING.md.
const FF_ID = 'appblocker@local';
const FF_XPI_URL = 'https://addons.mozilla.org/firefox/downloads/latest/appblocker-web-filter/latest.xpi';

function run(cmd, args) {
  return new Promise(resolve => execFile(cmd, args, () => resolve()));
}

async function apply(enabled) {
  if (process.platform === 'win32') return applyWindows(enabled);
  if (process.platform === 'darwin') return applyMac(enabled);
}

async function applyWindows(enabled) {
  const bases = [
    'HKCU\\Software\\Policies\\Google\\Chrome',
    'HKCU\\Software\\Policies\\Microsoft\\Edge',
    'HKCU\\Software\\Policies\\BraveSoftware\\Brave'
  ];
  for (const base of bases) {
    const key = base + '\\ExtensionInstallForcelist';
    if (enabled) {
      await run('reg', ['add', key, '/v', '1', '/t', 'REG_SZ', '/d', FORCE_VALUE, '/f']);
    } else {
      await run('reg', ['delete', key, '/v', '1', '/f']);
    }
  }
  // Firefox (only once an AMO URL is configured).
  if (FF_XPI_URL) {
    const fk = 'HKLM\\SOFTWARE\\Policies\\Mozilla\\Firefox\\ExtensionSettings\\' + FF_ID;
    if (enabled) {
      await run('reg', ['add', fk, '/v', 'installation_mode', '/t', 'REG_SZ', '/d', 'force_installed', '/f']);
      await run('reg', ['add', fk, '/v', 'install_url', '/t', 'REG_SZ', '/d', FF_XPI_URL, '/f']);
    } else {
      await run('reg', ['delete', fk, '/f']);
    }
  }
}

async function applyMac(enabled) {
  const domains = ['com.google.Chrome', 'com.microsoft.Edge', 'com.brave.Browser'];
  for (const d of domains) {
    if (enabled) {
      await run('defaults', ['write', d, 'ExtensionInstallForcelist', '-array', FORCE_VALUE]);
    } else {
      await run('defaults', ['delete', d, 'ExtensionInstallForcelist']);
    }
  }
}

module.exports = { apply, EXT_ID, UPDATE_URL };
