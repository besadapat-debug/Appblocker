package com.example.appblocker

import android.app.Application
import com.example.appblocker.cloud.CloudSync

/** Boots the cloud layer once for the whole process. All existing on-device
 *  features keep working with no account — sign-in only adds sync. */
class AppBlockerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudSync.init(this)
    }
}
