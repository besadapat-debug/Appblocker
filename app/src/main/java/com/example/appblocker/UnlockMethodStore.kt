package com.example.appblocker

import android.content.Context
import java.security.MessageDigest

enum class UnlockMethod(val label: String, val difficulty: String) {
    TIMER("Timer", "HARD"),
    FOLLOW_SCHEDULES("Follow schedules", "HARD"),
    CHARGER("Charger", "MEDIUM"),
    COOLDOWN("Cooldown", "MEDIUM"),
    PIN_CODE("PIN code", "EASY")
}

// Unlock configuration. Keys are suffixed with a `scope` so the master lock
// (scope = "") and each per-list / adult lock keep their own methods, timer,
// PIN, cooldown and activation time independently.
object UnlockMethodStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY_SELECTED_METHODS = "unlock_selected_methods"
    private const val KEY_TIMER_MINUTES = "unlock_timer_minutes"
    private const val KEY_COOLDOWN_SECONDS = "unlock_cooldown_seconds"
    private const val KEY_PIN_HASH = "unlock_pin_hash"
    private const val KEY_ACTIVATED_AT = "strict_activated_at"
    private const val KEY_COOLDOWN_STARTED_AT = "unlock_cooldown_started_at"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun k(base: String, scope: String) = base + scope

    fun getSelectedMethods(context: Context, scope: String = ""): Set<UnlockMethod> {
        val names = prefs(context).getStringSet(k(KEY_SELECTED_METHODS, scope), emptySet()) ?: emptySet()
        return names.mapNotNull { name -> runCatching { UnlockMethod.valueOf(name) }.getOrNull() }.toSet()
    }

    fun setSelectedMethods(context: Context, methods: Set<UnlockMethod>, scope: String = "") {
        prefs(context).edit()
            .putStringSet(k(KEY_SELECTED_METHODS, scope), methods.map { it.name }.toSet())
            .apply()
    }

    fun getTimerMinutes(context: Context, scope: String = ""): Int =
        prefs(context).getInt(k(KEY_TIMER_MINUTES, scope), 30)

    fun setTimerMinutes(context: Context, minutes: Int, scope: String = "") =
        prefs(context).edit().putInt(k(KEY_TIMER_MINUTES, scope), minutes).apply()

    // Extend the running timer by adding minutes on top (never shortens it).
    fun addTimerMinutes(context: Context, extra: Int, scope: String = "") {
        if (extra <= 0) return
        setTimerMinutes(context, getTimerMinutes(context, scope) + extra, scope)
    }

    fun getCooldownSeconds(context: Context, scope: String = ""): Int =
        prefs(context).getInt(k(KEY_COOLDOWN_SECONDS, scope), 15)

    fun setCooldownSeconds(context: Context, seconds: Int, scope: String = "") =
        prefs(context).edit().putInt(k(KEY_COOLDOWN_SECONDS, scope), seconds).apply()

    fun setPin(context: Context, pin: String, scope: String = "") {
        prefs(context).edit().putString(k(KEY_PIN_HASH, scope), hash(pin)).apply()
    }

    fun hasPinSet(context: Context, scope: String = ""): Boolean =
        prefs(context).contains(k(KEY_PIN_HASH, scope))

    fun checkPin(context: Context, pin: String, scope: String = ""): Boolean {
        val stored = prefs(context).getString(k(KEY_PIN_HASH, scope), null) ?: return false
        return stored == hash(pin)
    }

    fun recordActivatedNow(context: Context, scope: String = "") {
        prefs(context).edit().putLong(k(KEY_ACTIVATED_AT, scope), System.currentTimeMillis()).apply()
    }

    fun getActivatedAt(context: Context, scope: String = ""): Long =
        prefs(context).getLong(k(KEY_ACTIVATED_AT, scope), 0L)

    fun startCooldown(context: Context, scope: String = "") {
        prefs(context).edit().putLong(k(KEY_COOLDOWN_STARTED_AT, scope), System.currentTimeMillis()).apply()
    }

    fun clearCooldown(context: Context, scope: String = "") {
        prefs(context).edit().remove(k(KEY_COOLDOWN_STARTED_AT, scope)).apply()
    }

    fun getCooldownStartedAt(context: Context, scope: String = ""): Long =
        prefs(context).getLong(k(KEY_COOLDOWN_STARTED_AT, scope), 0L)

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
