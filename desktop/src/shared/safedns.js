// Adult content filter, done the light way: browser policies that make Chrome,
// Edge, Brave and Firefox look up websites through a family-safe DNS service
// (Cloudflare for Families: blocks adult and malware sites, millions of them,
// kept up to date by Cloudflare). Google SafeSearch is forced on too.
//
// Why not the hosts file? A 60,000+ line hosts file makes Windows' DNS Client
// crawl, so every non-Chromium program on the PC (Firefox, installers, updates)
// seemed to hang. This touches nothing system-wide: no hosts bloat, no network
// adapter changes, so it can't take the PC's internet down.
//
// "automatic" mode = if the family DNS can't be reached on some network, the
// browser falls back to normal DNS instead of losing the internet.
//
// Policies are re-applied every minute while the app/helper runs, so deleting
// the registry values only works until the next check. Browsers we can't set
// policies for (Opera, Vivaldi, ...) are handled by browserwatch when the web
// filter is on.
const { execFile } = require('child_process');

const DOH_URL = 'https://family.cloudflare-dns.com/dns-query';
const REAPPLY_MS = 60 * 1000;

const CHROMIUM_KEYS = [
  'SOFTWARE\\Policies\\Google\\Chrome',
  'SOFTWARE\\Policies\\Microsoft\\Edge',
  'SOFTWARE\\Policies\\BraveSoftware\\Brave'
];
const FIREFOX_KEY = 'SOFTWARE\\Policies\\Mozilla\\Firefox\\DNSOverHTTPS';

const MAC_CHROMIUM = ['com.google.Chrome', 'com.microsoft.Edge', 'com.brave.Browser'];

let lastState = null;
let lastApplied = 0;
let busy = false;

function run(cmd, args) {
  return new Promise(resolve => execFile(cmd, args, { windowsHide: true }, (err) => resolve(!err)));
}

// HKLM needs admin (the app runs elevated); fall back to HKCU if that fails.
async function regAdd(sub, name, type, data) {
  if (await run('reg', ['add', `HKLM\\${sub}`, '/v', name, '/t', type, '/d', String(data), '/f'])) return true;
  return run('reg', ['add', `HKCU\\${sub}`, '/v', name, '/t', type, '/d', String(data), '/f']);
}
async function regDel(sub, name) {
  await run('reg', ['delete', `HKLM\\${sub}`, '/v', name, '/f']);
  await run('reg', ['delete', `HKCU\\${sub}`, '/v', name, '/f']);
}

async function applyWindows(on) {
  for (const k of CHROMIUM_KEYS) {
    if (on) {
      await regAdd(k, 'DnsOverHttpsMode', 'REG_SZ', 'automatic');
      await regAdd(k, 'DnsOverHttpsTemplates', 'REG_SZ', DOH_URL);
      await regAdd(k, 'ForceGoogleSafeSearch', 'REG_DWORD', 1);
    } else {
      await regDel(k, 'DnsOverHttpsMode');
      await regDel(k, 'DnsOverHttpsTemplates');
      await regDel(k, 'ForceGoogleSafeSearch');
    }
  }
  if (on) {
    await regAdd(FIREFOX_KEY, 'Enabled', 'REG_DWORD', 1);
    await regAdd(FIREFOX_KEY, 'ProviderURL', 'REG_SZ', DOH_URL);
    await regAdd(FIREFOX_KEY, 'Fallback', 'REG_DWORD', 1);
    await regAdd(FIREFOX_KEY, 'Locked', 'REG_DWORD', 1);
  } else {
    for (const v of ['Enabled', 'ProviderURL', 'Fallback', 'Locked']) await regDel(FIREFOX_KEY, v);
  }
}

async function applyMac(on) {
  for (const d of MAC_CHROMIUM) {
    if (on) {
      await run('defaults', ['write', d, 'DnsOverHttpsMode', '-string', 'automatic']);
      await run('defaults', ['write', d, 'DnsOverHttpsTemplates', '-string', DOH_URL]);
      await run('defaults', ['write', d, 'ForceGoogleSafeSearch', '-bool', 'true']);
    } else {
      for (const k of ['DnsOverHttpsMode', 'DnsOverHttpsTemplates', 'ForceGoogleSafeSearch']) await run('defaults', ['delete', d, k]);
    }
  }
  if (on) {
    await run('defaults', ['write', 'org.mozilla.firefox', 'EnterprisePoliciesEnabled', '-bool', 'true']);
    await run('defaults', ['write', 'org.mozilla.firefox', 'DNSOverHTTPS', '-dict',
      'Enabled', '-bool', 'true', 'ProviderURL', '-string', DOH_URL, 'Fallback', '-bool', 'true', 'Locked', '-bool', 'true']);
  } else {
    await run('defaults', ['delete', 'org.mozilla.firefox', 'DNSOverHTTPS']);
  }
}

// Call often (every engine tick). Only touches the system when the wanted state
// changes, or once a minute while ON to undo tampering.
async function sync(on) {
  on = !!on;
  const now = Date.now();
  if (busy) return;
  if (lastState === on && (!on || now - lastApplied < REAPPLY_MS)) return;
  busy = true;
  try {
    if (process.platform === 'win32') await applyWindows(on);
    else if (process.platform === 'darwin') await applyMac(on);
    lastState = on;
    lastApplied = now;
  } catch { /* try again next tick */ }
  finally { busy = false; }
}

module.exports = { sync, DOH_URL };
