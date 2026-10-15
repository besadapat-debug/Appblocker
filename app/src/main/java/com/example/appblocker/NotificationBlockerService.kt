package com.example.appblocker

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

// Removes notifications from apps covered by an active list that has
// "Block notifications" switched on.
class NotificationBlockerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            activeNotifications?.forEach { handle(it) }
        } catch (e: Exception) {
            // listener not fully bound yet
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { handle(it) }
    }

    private fun handle(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == packageName || sbn.isOngoing) return
        if (BlockEngine.shouldSilenceNotifications(applicationContext, pkg)) {
            cancelNotification(sbn.key)
            UsageTracker.recordNotification(applicationContext, pkg)
        }
    }
}
