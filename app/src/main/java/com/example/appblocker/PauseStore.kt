package com.example.appblocker

import android.content.Context

// Short breaks from all blocking, limited per day. Disabled in Strict Mode.
object PauseStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY_PAUSED_UNTIL = "paused_until"
    private const val KEY_USED_DAY = "breaks_used_day"
    private const val KEY_USED_COUNT = "breaks_used_count"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPaused(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        now < pausedUntil(context)

    fun pausedUntil(context: Context): Long = prefs(context).getLong(KEY_PAUSED_UNTIL, 0L)

    fun breaksUsedToday(context: Context, now: Long = System.currentTimeMillis()): Int {
        val p = prefs(context)
        return if (p.getString(KEY_USED_DAY, null) == UsageTracker.dayKey(now)) p.getInt(KEY_USED_COUNT, 0) else 0
    }

    fun breaksLeft(context: Context, now: Long = System.currentTimeMillis()): Int =
        (AppSettings.breaksPerDay(context) - breaksUsedToday(context, now)).coerceAtLeast(0)

    fun canPause(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        !StrictModeStore.isActive(context) && !isPaused(context, now) && breaksLeft(context, now) > 0

    fun startPause(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        if (!canPause(context, now)) return false
        val used = breaksUsedToday(context, now) + 1
        prefs(context).edit()
            .putLong(KEY_PAUSED_UNTIL, now + AppSettings.breakMinutes(context) * 60_000L)
            .putString(KEY_USED_DAY, UsageTracker.dayKey(now))
            .putInt(KEY_USED_COUNT, used)
            .apply()
        return true
    }

    fun endPause(context: Context) {
        prefs(context).edit().putLong(KEY_PAUSED_UNTIL, 0L).apply()
    }
}
