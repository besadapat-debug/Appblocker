package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telecom.TelecomManager

object AppCatalog {

    @Volatile
    private var homeCache: Set<String>? = null

    @Volatile
    private var launchableCache: Set<String>? = null

    // Every app with a launcher icon, excluding AppBlocker itself.
    fun launcherApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    // Packages that are real, openable apps (have a launcher icon). Used so
    // allow-list mode blocks only actual apps, never background system services
    // (e.g. Samsung "Personal data intelligence") that briefly appear over an
    // allowed app and would otherwise kick the user out of it.
    fun isLaunchableApp(context: Context, pkg: String): Boolean {
        launchableCache?.let { return pkg in it }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val set = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.toHashSet()
        launchableCache = set
        return pkg in set
    }

    // Home screen apps (launchers).
    fun homePackages(context: Context): Set<String> {
        homeCache?.let { return it }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val set = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.toSet()
        homeCache = set
        return set
    }

    fun keyboardPackage(context: Context): String? =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')

    private val ALWAYS_ALLOWED = setOf(
        "android", "com.android.systemui", "com.android.settings",
        "com.android.phone", "com.android.server.telecom", "com.android.dialer",
        "com.google.android.dialer", "com.samsung.android.dialer", "com.android.emergency",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.android.packageinstaller", "com.google.android.packageinstaller"
    )

    // Apps an allow-list never blocks, so the phone stays usable.
    fun essentialPackages(context: Context): Set<String> {
        val set = HashSet(ALWAYS_ALLOWED)
        set.add(context.packageName)
        set.addAll(homePackages(context))
        keyboardPackage(context)?.let { set.add(it) }
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecom.defaultDialerPackage?.let { set.add(it) }
        } catch (e: Exception) {
            // no telephony on this device
        }
        return set
    }

    // Whether time in this app should count towards screen time / opens.
    fun isCountable(context: Context, pkg: String): Boolean =
        pkg != context.packageName && pkg != "android" && pkg != "com.android.systemui" &&
            pkg !in homePackages(context) && pkg != keyboardPackage(context)

    fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun label(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }

    fun simplify(label: String): String = label.lowercase().filter { it.isLetterOrDigit() }
}
