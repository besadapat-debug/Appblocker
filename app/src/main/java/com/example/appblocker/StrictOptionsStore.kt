package com.example.appblocker

import android.content.Context

// The set of things Strict Mode locks down, matching AppBlock's
// "Select options" screen. Two of these (recent apps / split screen)
// are stored for UI parity but are NOT enforced - see MainActivity's
// StrictOptionsActivity comments for why.
object StrictOptionsStore {

    private const val PREFS_NAME = "app_blocker_prefs"

    private const val KEY_LOCK_BLOCKINGS_IN_APP = "opt_lock_blockings_in_app"
    private const val KEY_DISABLE_UNINSTALL = "opt_disable_uninstall"
    private const val KEY_BLOCK_DEVICE_SETTINGS = "opt_block_device_settings"
    private const val KEY_BLOCK_RECENT_APPS = "opt_block_recent_apps" // UI only, not enforced
    private const val KEY_BLOCK_SPLIT_SCREEN = "opt_block_split_screen" // UI only, not enforced
    private const val KEY_BLOCK_REINSTALLED_APPS = "opt_block_reinstalled_apps"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLockBlockingsInApp(context: Context) = prefs(context).getBoolean(KEY_LOCK_BLOCKINGS_IN_APP, false)
    fun setLockBlockingsInApp(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_LOCK_BLOCKINGS_IN_APP, value).apply()

    fun isDisableUninstall(context: Context) = prefs(context).getBoolean(KEY_DISABLE_UNINSTALL, true)
    fun setDisableUninstall(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_DISABLE_UNINSTALL, value).apply()

    fun isBlockDeviceSettings(context: Context) = prefs(context).getBoolean(KEY_BLOCK_DEVICE_SETTINGS, true)
    fun setBlockDeviceSettings(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLOCK_DEVICE_SETTINGS, value).apply()

    fun isBlockRecentApps(context: Context) = prefs(context).getBoolean(KEY_BLOCK_RECENT_APPS, true)
    fun setBlockRecentApps(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLOCK_RECENT_APPS, value).apply()

    fun isBlockSplitScreen(context: Context) = prefs(context).getBoolean(KEY_BLOCK_SPLIT_SCREEN, true)
    fun setBlockSplitScreen(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLOCK_SPLIT_SCREEN, value).apply()

    fun isBlockReinstalledApps(context: Context) = prefs(context).getBoolean(KEY_BLOCK_REINSTALLED_APPS, true)
    fun setBlockReinstalledApps(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLOCK_REINSTALLED_APPS, value).apply()
}
