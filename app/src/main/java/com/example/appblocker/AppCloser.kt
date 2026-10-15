package com.example.appblocker

import android.app.ActivityManager
import android.content.Context

// Closes other apps' background processes. Android doesn't let a normal app
// remove entries from the Recents list, but it can ask the system to stop
// each app's background work, which is what "close all" effectively does.
object AppCloser {

    fun closeBackgroundApps(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val essentials = AppCatalog.essentialPackages(context)
        var count = 0
        for (app in AppCatalog.launcherApps(context)) {
            val pkg = app.packageName
            if (pkg == context.packageName || pkg in essentials) continue
            try {
                am.killBackgroundProcesses(pkg)
                count++
            } catch (e: Exception) {
                // some apps can't be touched - skip
            }
        }
        return count
    }
}
