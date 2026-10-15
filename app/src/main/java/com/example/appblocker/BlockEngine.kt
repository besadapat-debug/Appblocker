package com.example.appblocker

import java.util.Calendar
import android.content.Context

data class BlockDecision(
    val title: String,
    val subtitle: String,
    val untilText: String? = null
)

// The one place that decides "is this blocked right now, and why".
// Used by the accessibility service, the notification blocker and the UI.
object BlockEngine {

    // The Settings app goes by different package names across brands (stock,
    // Samsung One UI, Xiaomi, etc.). Any of these counts as "device settings".
    private val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
        "com.miui.securitycenter",
        "com.miui.settings",
        "com.oplus.settings",
        "com.coloros.settings",
        "com.oneplus.settings",
        "com.transsion.settings",
        "com.vivo.settings"
    )

    fun isSettingsPackage(pkg: String): Boolean =
        pkg in SETTINGS_PACKAGES || pkg.endsWith(".settings")

    // ----- List state -----

    fun isListBlocking(context: Context, list: BlockList, now: Long = System.currentTimeMillis()): Boolean {
        if (list.isQuick) return !list.isEmpty() && QuickBlockStore.isRunning(context, now)
        if (!list.enabled) return false
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        if (!list.isWithinSchedule(cal)) return false
        return limitReached(context, list, now)
    }

    fun blockingLists(context: Context, now: Long = System.currentTimeMillis()): List<BlockList> {
        if (PauseStore.isPaused(context, now)) return emptyList()
        return BlockListStore.getAll(context).filter { isListBlocking(context, it, now) }
    }

    // Today's total time (ms) and opens of the apps this list covers.
    fun usageFor(context: Context, list: BlockList, now: Long = System.currentTimeMillis()): Pair<Long, Int> {
        val essentials = AppCatalog.essentialPackages(context)
        var ms = 0L
        var opens = 0
        for ((pkg, s) in UsageTracker.today(context, now)) {
            if (list.targetsApp(pkg, essentials)) {
                ms += s.timeMs
                opens += s.launches
            }
        }
        return ms to opens
    }

    fun limitReached(context: Context, list: BlockList, now: Long = System.currentTimeMillis()): Boolean {
        if (!list.hasLimits()) return true
        val (ms, opens) = usageFor(context, list, now)
        if (list.usageLimitMinutes > 0 && ms >= list.usageLimitMinutes * 60_000L) return true
        if (list.launchLimit > 0 && opens > list.launchLimit) return true
        return false
    }

    // ----- Decisions -----

    fun decisionForApp(context: Context, pkg: String, now: Long = System.currentTimeMillis()): BlockDecision? {
        if (PauseStore.isPaused(context, now)) return null

        val essentials = AppCatalog.essentialPackages(context)
        val list = blockingLists(context, now).firstOrNull { candidate ->
            // Allow-list mode: only ever block real, openable apps - not hidden
            // system services that flash over an allowed app.
            if (candidate.allowMode && !AppCatalog.isLaunchableApp(context, pkg)) false
            else candidate.targetsApp(pkg, essentials)
        } ?: return null
        return BlockDecision(
            title = "${AppCatalog.label(context, pkg)} is blocked",
            subtitle = reason(list),
            untilText = untilText(context, list, now)
        )
    }

    fun decisionForAddress(context: Context, host: String, addressText: String, now: Long = System.currentTimeMillis()): BlockDecision? {
        // Porn blocker runs regardless of lists, schedules or breaks.
        if (AdultBlockStore.isEnabled(context)) {
            AdultBlocklist.blockReason(context, host, addressText)?.let { rule ->
                return BlockDecision(
                    title = "Adult content is blocked",
                    subtitle = if (rule == "adult content") "Adult content filter" else "$rule · Adult content filter"
                )
            }
        }
        for (list in blockingLists(context, now)) {
            val rule = list.blockedAddressRule(host, addressText) ?: continue
            val keyword = rule.startsWith("\"")
            return BlockDecision(
                title = if (keyword) "This page is blocked" else "$rule is blocked",
                subtitle = if (keyword) "Contains $rule · ${reason(list)}" else reason(list),
                untilText = untilText(context, list, now)
            )
        }
        return null
    }

    // A browser we can't read the address bar of, while the porn blocker wants
    // unsupported browsers blocked.
    fun shouldBlockUnsupportedBrowser(context: Context, pkg: String): BlockDecision? {
        if (!AdultBlockStore.isEnabled(context)) return null
        if (!AdultBlockStore.blockUnsupportedBrowsers(context)) return null
        if (!BrowserUrlReader.isKnownBrowserPackage(pkg)) return null
        if (BrowserUrlReader.isSupportedBrowser(pkg)) return null
        return BlockDecision(
            title = "${AppCatalog.label(context, pkg)} is blocked",
            subtitle = "The adult content filter can't check this browser, so it's blocked. Use Chrome, Firefox, Samsung Internet or Edge."
        )
    }

    fun shouldSilenceNotifications(context: Context, pkg: String): Boolean {
        val lists = blockingLists(context).filter { it.blockNotifications }
        if (lists.isEmpty()) return false
        val essentials = AppCatalog.essentialPackages(context)
        return lists.any { it.targetsApp(pkg, essentials) }
    }

    // For Strict Mode's "Follow schedules" unlock method.
    fun anyScheduledListBlocking(context: Context): Boolean =
        blockingLists(context).any { !it.isQuick && (it.scheduleEnabled || it.hasLimits()) }

    // ----- Text -----

    private fun reason(list: BlockList): String = when {
        list.isQuick -> "Quick Block"
        list.hasLimits() -> "Daily limit reached · \"${list.displayName()}\""
        else -> "Blocked by \"${list.displayName()}\""
    }

    fun untilText(context: Context, list: BlockList, now: Long = System.currentTimeMillis()): String? {
        if (list.isQuick) {
            val until = QuickBlockStore.untilMillis(context)
            return if (until == 0L) "Until you stop Quick Block" else "Until ${ScheduleFormat.moment(until, now)}"
        }
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val candidates = mutableListOf<Long>()
        list.windowEndMillis(cal)?.let { candidates.add(it) }
        if (list.hasLimits()) candidates.add(nextMidnight(now))
        val end = candidates.minOrNull() ?: return null
        return "Unblocks ${ScheduleFormat.moment(end, now)}"
    }

    fun nextMidnight(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
