package com.example.appblocker

import android.content.Context

object AppSettings {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY_BLOCK_MESSAGE = "block_screen_message"
    private const val KEY_BREAK_MINUTES = "break_minutes"
    private const val KEY_BREAKS_PER_DAY = "breaks_per_day"
    private const val KEY_STARTUP_LOCK_SECONDS = "startup_lock_seconds"

    val STARTUP_LOCK_CHOICES = listOf(5, 10, 15, 30, 60)

    val BREAK_LENGTH_CHOICES = listOf(1, 3, 5, 10, 15)
    val BREAKS_PER_DAY_CHOICES = listOf(0, 1, 2, 3, 5, 10)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun blockMessage(context: Context): String = prefs(context).getString(KEY_BLOCK_MESSAGE, "") ?: ""
    fun setBlockMessage(context: Context, value: String) =
        prefs(context).edit().putString(KEY_BLOCK_MESSAGE, value.trim()).apply()

    fun breakMinutes(context: Context): Int = prefs(context).getInt(KEY_BREAK_MINUTES, 5)
    fun setBreakMinutes(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_BREAK_MINUTES, value).apply()

    fun breaksPerDay(context: Context): Int = prefs(context).getInt(KEY_BREAKS_PER_DAY, 3)
    fun setBreaksPerDay(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_BREAKS_PER_DAY, value).apply()

    // Minimum time the "Getting your blocks ready" screen stays up after a restart.
    fun startupLockSeconds(context: Context): Int = prefs(context).getInt(KEY_STARTUP_LOCK_SECONDS, 10)
    fun setStartupLockSeconds(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_STARTUP_LOCK_SECONDS, value).apply()
}
