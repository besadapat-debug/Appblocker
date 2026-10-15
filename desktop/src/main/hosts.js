// Website blocking via the system hosts file. We manage our own marked section
// and map each blocked hostname to 0.0.0.0 so it fails to resolve in every
// browser. Editing hosts needs admin/root (the app requests elevation).
//
// IMPORTANT: the hosts file must stay SMALL. Windows' DNS Client service re-reads
// and indexes the whole file, and with tens of thousands of lines it slows every
// lookup on the PC to a crawl - Firefox, installers, Windows Update and other apps
// appear to "hang" while Chromium browsers (which parse hosts themselves) still
// work. So only block-list sites go here (expanded to their subdomains), never the
// adult list - that is handled by safe DNS in the browsers instead (see safedns.js).
const fs = require('fs');
const os = require('os');
const { execFile } = require('child_process');
const sites = require('../shared/sites');

const HOSTS_PATH = process.platform === 'win32'
  ? 'C:\\Windows\\System32\\drivers\\etc\\hosts'
  : '/etc/hosts';

const BEGIN = '# >>> AppBlocker begin >>>';
const END = '# <<< AppBlocker end <<<';
const MAX_HOSTS = 3000;   // hard safety cap - far above any normal block list

function stripSection(text) {
  // Remove every AppBlocker section (there could be more than one if a write was
  // interrupted), keeping everything the user or other software put there.
  let out = text;
  for (let guard = 0; guard < 10; guard++) {
    const b = out.indexOf(BEGIN);
    if (b === -1) break;
    const e = out.indexOf(END, b);
    out = e === -1 ? out.slice(0, b) : out.slice(0, b) + out.slice(e + END.length);
  }
  out = out.replace(/\r\n/g, '\n').replace(/\n{3,}/g, '\n\n').replace(/\s+$/, '');
  return (out ? out.split('\n').join(os.EOL) : '') + os.EOL;
}

function buildSection(hostsList) {
  if (!hostsList.length) return '';
  const lines = ['', BEGIN, '# Do not edit between these markers - managed by AppBlocker.'];
  for (const h of hostsList.slice(0, MAX_HOSTS)) {
    lines.push(`0.0.0.0 ${h}`);
    lines.push(`:: ${h}`); // IPv6 too, so nothing slips through on IPv6 networks
  }
  lines.push(END);
  return lines.join(os.EOL) + os.EOL;
}

// Apply the exact set of sites (as the user typed them) that should be blocked now.
function applyDomains(domains) {
  let current;
  try { current = fs.readFileSync(HOSTS_PATH, 'utf8'); }
  catch (e) { return { ok: false, error: 'Cannot read hosts file: ' + e.message }; }

  const hostsList = sites.expandAll(domains);
  const next = stripSection(current) + buildSection(hostsList);
  if (next === current) return { ok: true, changed: false, count: hostsList.length };

  // The hosts file is often briefly locked (another writer, antivirus scan).
  // Retry a few times before surfacing the error.
  let lastErr = null;
  for (let attempt = 0; attempt < 4; attempt++) {
    try {
      fs.writeFileSync(HOSTS_PATH, next, 'utf8');
      flushDns();
      return { ok: true, changed: true, count: hostsList.length };
    } catch (e) {
      lastErr = e;
      if (e.code === 'EBUSY' || e.code === 'EPERM') { busyWait(120); continue; }
      break;
    }
  }
  const hint = (lastErr && lastErr.code === 'EBUSY')
    ? 'hosts file is locked (close other AppBlocker copies, or allow this app in your antivirus): '
    : 'need admin: ';
  return { ok: false, error: 'Cannot write hosts file (' + hint + (lastErr ? lastErr.message : 'unknown') + ')' };
}

// Short synchronous pause (rare: only between hosts-write retries).
function busyWait(ms) { const end = Date.now() + ms; while (Date.now() < end) { /* spin */ } }

function clear() { return applyDomains([]); }

function flushDns() {
  if (process.platform === 'win32') {
    execFile('ipconfig', ['/flushdns'], { windowsHide: true }, () => {});
  } else if (process.platform === 'darwin') {
    execFile('dscacheutil', ['-flushcache'], () => {});
    execFile('killall', ['-HUP', 'mDNSResponder'], () => {});
  }
}

module.exports = { applyDomains, clear, HOSTS_PATH, stripSection, buildSection };
