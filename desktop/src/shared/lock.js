// A reusable "lock" with its OWN unlock methods, shared by the global Strict
// Mode, each block list's own Strict Mode, and the adult/web filter's own
// Strict Mode. Mirrors the Android per-lock unlock system.
//
// Methods: 'password', 'timer', 'cooldown', 'schedule' (follow-schedules).
// Each lock keeps its own settings so they're all independent.
const crypto = require('crypto');
const { rangeActive } = require('./schedule');

function hash(pw) {
  return crypto.createHash('sha256').update(String(pw)).digest('hex');
}

function defaultLock() {
  return {
    enabled: false,
    methods: [],
    passwordHash: '',
    waitMinutes: 0,
    cooldownSeconds: 0,
    ranges: [],
    activatedAt: 0,
    cooldownStartedAt: 0
  };
}

// Normalise any stored value into a full lock object.
function normalize(lock) {
  return Object.assign(defaultLock(), lock || {});
}

function isOn(lock) {
  return !!(lock && lock.enabled);
}

function withinOwnSchedule(lock, now) {
  const ranges = lock.ranges || [];
  // THE FIX (matches Android): "Follow schedules" can only release the lock
  // when you're OUTSIDE the schedule. If NO schedule is set at all, the lock
  // must STAY ON rather than fall open. So: no ranges -> treat as "within".
  if (!ranges.length) return true;
  return ranges.some(r => rangeActive(r, now));
}

// Turn a lock on. activatedAt starts the timer.
function activate(lock, patch) {
  const next = normalize(Object.assign({}, lock, patch));
  next.enabled = true;
  next.activatedAt = Date.now();
  next.cooldownStartedAt = 0;
  return next;
}

// Attempt to unlock. Returns { ok, reason, unlockAt, lock } where `lock` is the
// (possibly mutated) lock state the caller should persist.
function attemptUnlock(lock, { password = '', now = Date.now() } = {}) {
  let l = normalize(lock);
  if (!l.enabled) return { ok: true, lock: l };

  const methods = l.methods || [];
  if (!methods.length) {
    // No unlock method chosen -> off instantly.
    l = Object.assign({}, l, { enabled: false, activatedAt: 0, cooldownStartedAt: 0 });
    return { ok: true, lock: l };
  }

  // Password
  if (methods.includes('password') && l.passwordHash) {
    if (hash(password || '') !== l.passwordHash) {
      return { ok: false, reason: 'Wrong password', lock: l };
    }
  }

  // Cooldown: press once to start the countdown, then wait it out.
  if (methods.includes('cooldown')) {
    const reqMs = (l.cooldownSeconds || 0) * 1000;
    if (reqMs > 0) {
      if (!l.cooldownStartedAt) {
        l = Object.assign({}, l, { cooldownStartedAt: now });
        return { ok: false, reason: 'cooldown', unlockAt: now + reqMs, lock: l };
      }
      if (now < l.cooldownStartedAt + reqMs) {
        return { ok: false, reason: 'cooldown', unlockAt: l.cooldownStartedAt + reqMs, lock: l };
      }
    }
  }

  // Timer: measured from activation.
  if (methods.includes('timer')) {
    const reqMs = (l.waitMinutes || 0) * 60000;
    const unlockAt = (l.activatedAt || 0) + reqMs;
    if (!l.activatedAt || now < unlockAt) {
      return { ok: false, reason: 'timer', unlockAt, lock: l };
    }
  }

  // Follow schedules. A schedule with NO ranges is a misconfiguration - it would
  // lock forever with no buffer window, which violates the whole design ("there
  // is always a window to turn things off"). So an empty schedule does NOT hold
  // the lock; only a real schedule window does, and only while you're inside it.
  if (methods.includes('schedule')) {
    const hasRanges = (l.ranges || []).length > 0;
    if (hasRanges && withinOwnSchedule(l, new Date(now))) {
      return { ok: false, reason: 'schedule', lock: l };
    }
  }

  // All conditions met.
  l = Object.assign({}, l, { enabled: false, activatedAt: 0, cooldownStartedAt: 0 });
  return { ok: true, lock: l };
}

// Human-readable reason for a failed unlock.
function reasonText(res) {
  switch (res.reason) {
    case 'Wrong password': return 'Wrong password';
    case 'cooldown': return 'Cooldown - unlocks at ' + new Date(res.unlockAt).toLocaleTimeString();
    case 'timer': return 'Timer - unlocks at ' + new Date(res.unlockAt).toLocaleTimeString();
    case 'schedule': return "Locked until you're outside your unlock schedule";
    case 'no-schedule': return 'No unlock schedule is set, so this stays locked';
    default: return res.reason || 'Locked';
  }
}

module.exports = { hash, defaultLock, normalize, isOn, activate, attemptUnlock, reasonText, withinOwnSchedule };
