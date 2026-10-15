// Decides whether a block list is active right now, from its weekly ranges.
// A range: { days:[0-6 (0=Sun)], start:"HH:MM", end:"HH:MM" }.
// A range crossing midnight (start > end) counts on the day it starts.
function minutesNow(d = new Date()) { return d.getHours() * 60 + d.getMinutes(); }

function toMin(hhmm) {
  const [h, m] = String(hhmm).split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
}

function rangeActive(range, d = new Date()) {
  const day = d.getDay();
  const prevDay = (day + 6) % 7;
  const now = minutesNow(d);
  const s = toMin(range.start), e = toMin(range.end);
  const days = range.days || [];
  if (s === e) return days.includes(day);          // all day
  if (s < e) return days.includes(day) && now >= s && now < e;
  // crosses midnight
  return (days.includes(day) && now >= s) || (days.includes(prevDay) && now < e);
}

function listActive(list, d = new Date()) {
  if (!list.enabled) return false;
  if (!list.scheduleEnabled) return true;          // always on while enabled
  const ranges = list.ranges || [];
  if (ranges.length === 0) return true;
  return ranges.some(r => rangeActive(r, d));
}

module.exports = { listActive, rangeActive };
