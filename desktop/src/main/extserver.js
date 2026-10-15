// A tiny localhost-only control server the companion browser extension talks to.
//
// The extension polls GET /config for the current web allow-list, and pings
// POST /heartbeat every few seconds so the app knows it's alive. If the
// extension is removed or disabled, the heartbeats stop and browserwatch closes
// the browser (which, with the force-install policy, reinstalls the extension
// on next launch). This avoids any native-messaging host install.
const http = require('http');
const fs = require('fs');
const path = require('path');
const { listActive } = require('../shared/schedule');
const sites = require('../shared/sites');

const PORT = 47113; // localhost only
const CRX_PATH = path.join(__dirname, '..', 'extension', 'appblocker.crx');
const EXT_ID = 'bghhcnjlkiacnebfdfkpmihgnlohglci';
const CRX_VERSION = '1.0.1'; // must match extension manifest.json "version"

let lastBeat = 0;
const beats = Object.create(null); // browserId ('chrome','brave','edge'...) -> last beat ms
let server = null;

function start(store) {
  stop();
  server = http.createServer((req, res) => {
    // Hard CORS allow for the extension origin(s); localhost only anyway.
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', 'content-type');
    if (req.method === 'OPTIONS') { res.writeHead(204); return res.end(); }

    if (req.url.startsWith('/config')) {
      const s = store.get();
      // Websites from every CURRENTLY-ACTIVE block list (schedules respected).
      // The extension blocks these in-browser, so block lists work even when
      // the browser uses encrypted DNS (DoH) that bypasses the hosts file.
      const blockDomains = [];
      const blockExceptions = [];
      for (const l of (s.lists || [])) {
        if (!listActive(l)) continue;
        for (const w of (l.websites || [])) { const c = sites.clean(w); if (c) blockDomains.push(c); }
        for (const ex of (l.exceptions || [])) { const e = String(ex || '').trim(); if (e) blockExceptions.push(e); }
      }
      const body = JSON.stringify({
        webFilterEnabled: !!(s.webFilter && s.webFilter.enabled),
        allowedSites: (s.webFilter && s.webFilter.allowedSites) || [],
        blockDomains: [...new Set(blockDomains)],
        blockExceptions: [...new Set(blockExceptions)],
        adultEnabled: !!s.adultFilter
      });
      res.writeHead(200, { 'content-type': 'application/json' });
      return res.end(body);
    }

    if (req.url.startsWith('/heartbeat')) {
      lastBeat = Date.now();
      // The extension tags each beat with the browser it's running in (?b=brave)
      // so we know PER BROWSER whether the extension is still present. Removing
      // it from Brave while Chrome still has it must stop Brave's beats only.
      let b = '';
      try { b = new URL(req.url, 'http://x').searchParams.get('b') || ''; } catch {}
      if (b) beats[b.toLowerCase()] = lastBeat;
      res.writeHead(200, { 'content-type': 'application/json' });
      return res.end('{"ok":true}');
    }

    // Self-hosted auto-update manifest so browsers FORCE-INSTALL the extension
    // on their own (no "Load unpacked"). The force-install policy points here.
    if (req.url.startsWith('/update.xml')) {
      const xml = `<?xml version='1.0' encoding='UTF-8'?>\n` +
        `<gupdate xmlns='http://www.google.com/update2/response' protocol='2.0'>\n` +
        `  <app appid='${EXT_ID}'>\n` +
        `    <updatecheck codebase='http://127.0.0.1:${PORT}/appblocker.crx' version='${CRX_VERSION}' />\n` +
        `  </app>\n</gupdate>\n`;
      res.writeHead(200, { 'content-type': 'application/xml' });
      return res.end(xml);
    }

    if (req.url.startsWith('/appblocker.crx')) {
      try {
        const buf = fs.readFileSync(CRX_PATH);
        res.writeHead(200, { 'content-type': 'application/x-chrome-extension', 'content-length': buf.length });
        return res.end(buf);
      } catch (e) { res.writeHead(404); return res.end(); }
    }

    res.writeHead(404);
    res.end();
  });
  server.on('error', () => { /* port busy or blocked - watch will just see no beats */ });
  try { server.listen(PORT, '127.0.0.1'); } catch (e) { /* ignore */ }
}

function stop() {
  if (server) { try { server.close(); } catch {} server = null; }
}

function lastHeartbeat() { return lastBeat; }
// Any browser connected recently (used only for the "ever connected" gate).
function isFresh(withinMs = 20000) { return Date.now() - lastBeat < withinMs; }
// Has THIS specific browser beaten recently? Stale => its extension is gone.
function isFreshFor(id, withinMs = 20000) {
  const t = beats[String(id).toLowerCase()] || 0;
  return Date.now() - t < withinMs;
}
// { browserId: millisecondsSinceLastBeat } for every browser ever seen. Lets the
// UI show, per browser, how long ago the extension last checked in.
function beatAges() {
  const now = Date.now();
  const out = {};
  for (const id of Object.keys(beats)) out[id] = now - beats[id];
  return out;
}

module.exports = { start, stop, lastHeartbeat, isFresh, isFreshFor, beatAges, PORT };
