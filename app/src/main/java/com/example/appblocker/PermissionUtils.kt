package com.example.appblocker

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object PermissionUtils {

    fun hasAccessibilityPermission(context: Context): Boolean {
        val expected = "${context.packageName}/${AppBlockAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasNotificationAccess(context: Context): Boolean {
        val expected = ComponentName(context, NotificationBlockerService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
