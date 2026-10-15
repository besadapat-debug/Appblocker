package com.example.appblocker

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

// While this app is an active Device Admin, Android requires the admin to
// be deactivated (Settings > Security > Device admin apps) before
// AppBlocker itself can be uninstalled. That's the real, OS-enforced
// mechanism behind AppBlock's "Disable AppBlock uninstalling" toggle.
class BlockerDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "AppBlocker admin access removed", Toast.LENGTH_SHORT).show()
    }
}
