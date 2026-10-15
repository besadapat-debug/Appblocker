// The big (global) Strict Mode, now built on the shared per-lock unlock engine
// so it supports password, timer, cooldown and follow-schedules - the same set
// each block list and the adult/web filter get for their own locks.
//
// Honest limits are unchanged (an admin can still edit the config/hosts by
// hand) - see README.
const lock = require('../shared/lock');

function activate(store, opts = {}) {
  const s = store.get();
  s.strict = lock.activate(s.strict, {
    methods: opts.methods || s.strict.methods || [],
    passwordHash: opts.password ? lock.hash(opts.password) : (s.strict.passwordHash || ''),
    waitMinutes: Number(opts.waitMinutes) || s.strict.waitMinutes || 0,
    cooldownSeconds: Number(opts.cooldownSeconds) || s.strict.cooldownSeconds || 0,
    ranges: opts.ranges || s.strict.ranges || []
  });
  store.set(s);
  return store.get();
}

function tryDeactivate(store, password) {
  const s = store.get();
  const res = lock.attemptUnlock(s.strict, { password });
  s.strict = res.lock;
  store.set(s);
  return { ok: res.ok, reason: res.ok ? undefined : lock.reasonText(res), unlockAt: res.unlockAt };
}

function isOn(store) {
  return lock.isOn(store.get().strict);
}

module.exports = { activate, tryDeactivate, isOn, hash: lock.hash };
