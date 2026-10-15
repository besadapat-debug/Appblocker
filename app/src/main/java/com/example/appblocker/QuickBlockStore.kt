package com.example.appblocker

import android.content.Context

// Quick Block: a one-off block of the Quick Block list, for a set time or
// until stopped. untilMillis == 0 means "until I stop it".
object QuickBlockStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY_RUNNING = "quick_running"
    private const val KEY_UNTIL = "quick_until"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isRunning(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val p = prefs(context)
        if (!p.getBoolean(KEY_RUNNING, false)) return false
        val until = p.getLong(KEY_UNTIL, 0L)
        if (until != 0L && now >= until) {
            stop(context)
            return false
        }
        return true
    }

    fun untilMillis(context: Context): Long = prefs(context).getLong(KEY_UNTIL, 0L)

    fun start(context: Context, untilMillis: Long) {
        prefs(context).edit().putBoolean(KEY_RUNNING, true).putLong(KEY_UNTIL, untilMillis).apply()
    }

    fun stop(context: Context) {
        prefs(context).edit().putBoolean(KEY_RUNNING, false).putLong(KEY_UNTIL, 0L).apply()
    }
}
