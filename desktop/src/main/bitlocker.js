// BitLocker: DETECT status and GUIDE the user to Windows' own encryption wizard.
// We never silently encrypt a drive - a lost recovery key means permanent data
// loss, so key backup must go through Windows' UI with the user's eyes open.
const { execFile } = require('child_process');

// Returns { supported, on, status } for the system drive via a callback.
function status(cb) {
  if (process.platform !== 'win32') return cb({ supported: false });
  const script =
    "try{$v=Get-BitLockerVolume -MountPoint $env:SystemDrive;" +
    "@{supported=$true;on=($v.ProtectionStatus -eq 'On');status=[string]$v.VolumeStatus}|ConvertTo-Json -Compress}" +
    "catch{@{supported=$false}|ConvertTo-Json -Compress}";
  execFile('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', script],
    { windowsHide: true, timeout: 15000 }, (err, out) => {
      if (err) return cb({ supported: false });
      try { cb(JSON.parse(String(out).trim())); } catch { cb({ supported: false }); }
    });
}

// Opens the Windows BitLocker control panel so the user can turn it on and save
// their recovery key the proper way.
function openSettings() {
  if (process.platform !== 'win32') return;
  execFile('control', ['/name', 'Microsoft.BitLockerDriveEncryption'], { windowsHide: true }, () => {});
}

module.exports = { status, openSettings };
