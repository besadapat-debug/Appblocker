package com.example.appblocker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayStat(
    var timeMs: Long = 0L,
    var launches: Int = 0,
    var blocked: Int = 0,
    var notifications: Int = 0
)

// AppBlocker's own screen-time counter, fed by the accessibility service.
// Powers daily usage/launch limits and the Insights tab. No Usage Access
// permission needed, and it updates instantly (every tick).
object UsageTracker {

    private const val PREFS_NAME = "usage_tracker"
    private const val KEEP_DAYS = 14
    private const val MAX_SESSION_CHUNK_MS = 60 * 60 * 1000L // ignore bogus gaps

    private val days = mutableMapOf<String, MutableMap<String, DayStat>>()
    private var currentPkg: String? = null
    private var sessionStart = 0L
    private var lastPkg: String? = null
    private var lastCountable = false

    fun dayKey(millis: Long): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(millis))

    // ----- Events from the accessibility service -----

    @Synchronized
    fun onForeground(context: Context, pkg: String, countable: Boolean, now: Long) {
        flush(context, now)
        if (countable && pkg != lastPkg) {
            stat(context, dayKey(now), pkg).launches++
        }
        currentPkg = if (countable) pkg else null
        sessionStart = now
        lastPkg = pkg
        lastCountable = countable
        save(context, dayKey(now))
    }

    @Synchronized
    fun onScreenOff(context: Context, now: Long) {
        flush(context, now)
        currentPkg = null
        save(context, dayKey(now))
    }

    // Unlocking the phone back into the same app is not a new "open".
    @Synchronized
    fun onUserPresent(now: Long) {
        if (lastCountable && lastPkg != null) {
            currentPkg = lastPkg
            sessionStart = now
        }
    }

    @Synchronized
    fun tick(context: Context, now: Long) {
        if (currentPkg == null) return
        flush(context, now)
        save(context, dayKey(now))
    }

    @Synchronized
    fun recordBlocked(context: Context, pkg: String, now: Long = System.currentTimeMillis()) {
        stat(context, dayKey(now), pkg).blocked++
        save(context, dayKey(now))
    }

    @Synchronized
    fun recordNotification(context: Context, pkg: String, now: Long = System.currentTimeMillis()) {
        stat(context, dayKey(now), pkg).notifications++
        save(context, dayKey(now))
    }

    // ----- Queries -----

    @Synchronized
    fun today(context: Context, now: Long = System.currentTimeMillis()): Map<String, DayStat> {
        flush(context, now)
        return load(context, dayKey(now)).mapValues { it.value.copy() }
    }

    // Totals over the last `count` days including today.
    @Synchronized
    fun lastDays(context: Context, count: Int, now: Long = System.currentTimeMillis()): Map<String, DayStat> {
        flush(context, now)
        val result = mutableMapOf<String, DayStat>()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        repeat(count) {
            for ((pkg, s) in load(context, dayKey(cal.timeInMillis))) {
                val t = result.getOrPut(pkg) { DayStat() }
                t.timeMs += s.timeMs
                t.launches += s.launches
                t.blocked += s.blocked
                t.notifications += s.notifications
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result
    }

    // ----- Internals -----

    private fun flush(context: Context, now: Long) {
        val pkg = currentPkg ?: return
        val elapsed = now - sessionStart
        if (elapsed in 1..MAX_SESSION_CHUNK_MS) {
            stat(context, dayKey(now), pkg).timeMs += elapsed
        }
        sessionStart = now
    }

    private fun stat(context: Context, day: String, pkg: String): DayStat =
        load(context, day).getOrPut(pkg) { DayStat() }

    private fun load(context: Context, day: String): MutableMap<String, DayStat> {
        days[day]?.let { return it }
        val map = mutableMapOf<String, DayStat>()
        val json = prefs(context).getString("d_$day", null)
        if (json != null) {
            try {
                val o = JSONObject(json)
                for (pkg in o.keys()) {
                    val a = o.getJSONArray(pkg)
                    map[pkg] = DayStat(a.getLong(0), a.getInt(1), a.getInt(2), a.getInt(3))
                }
            } catch (e: Exception) {
                // corrupt day - start fresh
            }
        }
        days[day] = map
        if (days.size > KEEP_DAYS + 2) days.keys.sorted().take(days.size - KEEP_DAYS).forEach { days.remove(it) }
        return map
    }

    private fun save(context: Context, day: String) {
        val map = days[day] ?: return
        val o = JSONObject()
        for ((pkg, s) in map) {
            o.put(pkg, JSONArray().put(s.timeMs).put(s.launches).put(s.blocked).put(s.notifications))
        }
        val p = prefs(context)
        val editor = p.edit().putString("d_$day", o.toString())
        // Drop days older than KEEP_DAYS.
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -KEEP_DAYS) }
        val cutoffKey = dayKey(cutoff.timeInMillis)
        p.all.keys.filter { it.startsWith("d_") && it.removePrefix("d_") < cutoffKey }.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
