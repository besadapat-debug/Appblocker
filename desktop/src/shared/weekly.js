// The "weekly unlock window": the single stretch of the week (default 1 minute)
// that the user chooses as the ONLY time Strict Mode / a frozen account can be
// turned off. Everything stays locked the rest of the week.
//
// window = { enabled, day (0=Sun..6=Sat), start:"HH:MM", minutes:int>=1 }
const MIN_PER_WEEK = 7 * 24 * 60;

function minutesOfWeek(d) { return d.getDay() * 1440 + d.getHours() * 60 + d.getMinutes(); }

function windowBounds(w) {
  const [h, m] = String(w.start || '03:00').split(':').map(Number);
  const start = (w.day || 0) * 1440 + (h || 0) * 60 + (m || 0);
  const len = Math.max(1, Number(w.minutes) || 1);
  return [start, start + len]; // end may exceed MIN_PER_WEEK (wraps past Sat midnight)
}

function withinWeeklyWindow(w, now = new Date()) {
  if (!w || !w.enabled) return false;
  const [s, e] = windowBounds(w);
  const t = minutesOfWeek(now);
  if (e <= MIN_PER_WEEK) return t >= s && t < e;
  return t >= s || t < (e - MIN_PER_WEEK); // wrapped window
}

// Next Date at which the window opens (for "unlocks at …" messages).
function nextWindowStart(w, now = new Date()) {
  const [h, m] = String(w.start || '03:00').split(':').map(Number);
  const target = new Date(now);
  const delta = (((w.day || 0) - now.getDay()) + 7) % 7;
  target.setDate(now.getDate() + delta);
  target.setHours(h || 0, m || 0, 0, 0);
  if (target.getTime() <= now.getTime()) target.setDate(target.getDate() + 7);
  return target;
}

module.exports = { withinWeeklyWindow, nextWindowStart, windowBounds };
