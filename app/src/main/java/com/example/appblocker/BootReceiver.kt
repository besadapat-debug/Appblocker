package com.example.appblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Phone restarted or AppBlocker was updated -> lock the screen until
// blocking is confirmed running.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                if (StartupGuard.isNeeded(context)) StartupGuard.show(context)
            }
        }
    }
}
