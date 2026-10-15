// App blocking: poll running processes and terminate any that match a blocked
// app. Cross-platform via built-in OS commands (no native deps).
const { execFile } = require('child_process');

function listProcesses() {
  return new Promise(resolve => {
    if (process.platform === 'win32') {
      // CSV: "Image Name","PID",...
      execFile('tasklist', ['/fo', 'csv', '/nh'], { maxBuffer: 1 << 24 }, (err, out) => {
        if (err) return resolve([]);
        const names = out.split(/\r?\n/).map(line => {
          const m = line.match(/^"([^"]+)"/);
          return m ? m[1].toLowerCase() : null;
        }).filter(Boolean);
        resolve(names);
      });
    } else {
      execFile('ps', ['-axco', 'comm'], { maxBuffer: 1 << 24 }, (err, out) => {
        if (err) return resolve([]);
        resolve(out.split(/\r?\n/).map(s => s.trim().toLowerCase()).filter(Boolean));
      });
    }
  });
}

function killByName(name) {
  const n = String(name).trim();
  if (!n) return;
  if (process.platform === 'win32') {
    const image = n.toLowerCase().endsWith('.exe') ? n : n + '.exe';
    execFile('taskkill', ['/IM', image, '/F', '/T'], () => {});
  } else {
    // match the process "comm" name exactly
    execFile('pkill', ['-x', n.replace(/\.app$/i, '')], () => {});
  }
}

// blockedNames: array of executable names ("instagram.exe", "Discord").
async function enforce(blockedNames) {
  if (!blockedNames.length) return;
  const running = await listProcesses();
  const wanted = blockedNames.map(s => {
    const x = s.toLowerCase();
    return process.platform === 'win32' && !x.endsWith('.exe') ? x + '.exe' : x;
  });
  for (const proc of running) {
    if (wanted.includes(proc)) killByName(proc);
  }
}

module.exports = { listProcesses, enforce, killByName };
