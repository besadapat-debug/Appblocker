// System-wide config location so a privileged helper (running as SYSTEM/root)
// and the per-user UI both read/write the same file. Needs admin to write.
const path = require('path');
const os = require('os');

function configDir() {
  if (process.platform === 'win32') {
    return path.join(process.env.ProgramData || 'C:\\ProgramData', 'AppBlocker');
  }
  return '/Library/Application Support/AppBlocker';
}

module.exports = {
  configDir,
  configFile: () => path.join(configDir(), 'config.json')
};
