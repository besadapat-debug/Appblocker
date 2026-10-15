package com.example.appblocker

import android.content.Context

// Tracks whether a "Strict Mode" lock is active. There is one MASTER lock
// (scope = "") plus an independent lock per block list and for the adult filter
// (see ListLocks). While a lock is active, the things it guards can only be made
// stricter, never weaker, until that specific lock is deactivated.
object StrictModeStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY_STRICT_MODE_ACTIVE = "strict_mode_active"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // scope "" = the master lock; "_list_<id>" / "_adult_" = an independent lock.
    private fun key(scope: String) = KEY_STRICT_MODE_ACTIVE + scope

    fun isActive(context: Context, scope: String = ""): Boolean =
        prefs(context).getBoolean(key(scope), false)

    fun setActive(context: Context, active: Boolean, scope: String = "") {
        prefs(context).edit().putBoolean(key(scope), active).apply()
    }

    // A master-guarded setting is "frozen" (can't be weakened) only if it was
    // already ON when the master lock started. One that was OFF can still be
    // switched ON.
    fun freezeOn(context: Context, alreadyOn: Boolean): Boolean = isActive(context) && alreadyOn
}
