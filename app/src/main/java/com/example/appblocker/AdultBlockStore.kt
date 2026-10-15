package com.example.appblocker

import android.content.Context

// The always-on porn blocker. Independent of block lists: once on, it blocks
// adult sites in every supported browser, and (optionally) blocks browsers
// that can't be checked so they can't be used as a loophole.
object AdultBlockStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY_ENABLED = "adult_block_enabled"
    private const val KEY_BLOCK_UNSUPPORTED = "adult_block_unsupported_browsers"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)
    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
        if (value) AdultBlocklist.ensureLoaded(context)
    }

    // Default ON: if we can't see the address bar, we can't guarantee safety,
    // so an unknown browser is blocked while the porn blocker is on.
    fun blockUnsupportedBrowsers(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_UNSUPPORTED, true)
    fun setBlockUnsupportedBrowsers(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCK_UNSUPPORTED, value).apply()
    }
}
