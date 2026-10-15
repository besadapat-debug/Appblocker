package com.example.appblocker

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

// One block list = a named group of apps / websites / keywords with its own
// switch, optional time schedule and optional daily limits.
//
// allowMode = false: block the selected apps & sites.
// allowMode = true:  block EVERYTHING except the selected apps (and, if any
//                    websites are listed, every site except those).
data class BlockList(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val allowMode: Boolean = false,
    val apps: Set<String> = emptySet(),
    val websites: Set<String> = emptySet(),
    val keywords: Set<String> = emptySet(),
    val scheduleEnabled: Boolean = false,
    // Blocks while ANY range covers the current time.
    val ranges: List<TimeRange> = listOf(TimeRange()),
    // 0 = no limit. With a limit, the list only starts blocking once today's
    // total usage / number of opens of its apps passes the limit.
    val usageLimitMinutes: Int = 0,
    val launchLimit: Int = 0,
    val blockNotifications: Boolean = false
) {

    val isQuick: Boolean get() = id == QUICK_ID

    fun hasLimits(): Boolean = usageLimitMinutes > 0 || launchLimit > 0

    fun isEmpty(): Boolean = !allowMode && apps.isEmpty() && websites.isEmpty() && keywords.isEmpty()

    fun displayName(): String = if (isQuick) "Quick Block" else name.ifBlank { "Untitled list" }

    // Time/day check only (true when there's no schedule).
    fun isWithinSchedule(now: Calendar = Calendar.getInstance()): Boolean =
        !scheduleEnabled || ranges.any { it.contains(now) }

    // When blocking by schedule stops, following back-to-back ranges
    // (e.g. 9-12 then 12-5 -> 5pm). Null if no schedule / not inside one.
    fun windowEndMillis(now: Calendar = Calendar.getInstance()): Long? {
        if (!scheduleEnabled || !isWithinSchedule(now)) return null
        var end = ranges.mapNotNull { it.endMillis(now) }.maxOrNull() ?: return null
        repeat(20) {
            val at = (now.clone() as Calendar).apply { timeInMillis = end }
            val next = ranges.mapNotNull { it.endMillis(at) }.maxOrNull() ?: return end
            if (next <= end) return end
            end = next
        }
        return end
    }

    // Does this list apply to the app? `essentials` (launcher, phone, settings,
    // keyboard, AppBlocker...) are never blocked by an allow-list.
    fun targetsApp(packageName: String, essentials: Set<String>): Boolean =
        if (allowMode) packageName !in apps && packageName !in essentials
        else packageName in apps

    // Returns what matched (a domain or a quoted keyword) or null if the
    // address isn't blocked by this list.
    fun blockedAddressRule(host: String, addressText: String): String? {
        matchingKeyword(addressText)?.let { return "\"$it\"" }
        return if (allowMode) {
            // Explicitly allowed sites always pass.
            if (websites.any { hostMatches(host, it) }) return null
            // Block the website of any app that isn't in the allowed list, so
            // "allow only these apps" also blocks the web version of the rest
            // (e.g. Instagram app not allowed -> instagram.com blocked too).
            val pkgs = LinkedTargets.packagesForDomain(host)
            if (pkgs.isNotEmpty() && pkgs.none { it in apps }) return host
            // Otherwise: empty allowed-sites list means all other sites are
            // allowed; a non-empty list blocks everything not on it.
            if (websites.isEmpty()) null else host
        } else {
            websites.firstOrNull { hostMatches(host, it) }
        }
    }

    fun matchingKeyword(addressText: String): String? {
        if (keywords.isEmpty()) return null
        val text = addressText.lowercase().replace("%20", " ").replace('+', ' ')
        return keywords.firstOrNull { text.contains(it) }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("enabled", enabled)
        put("allowMode", allowMode)
        put("apps", JSONArray(apps.toList()))
        put("websites", JSONArray(websites.toList()))
        put("keywords", JSONArray(keywords.toList()))
        put("scheduleEnabled", scheduleEnabled)
        put("ranges", JSONArray().apply { ranges.forEach { put(it.toJson()) } })
        put("usageLimitMinutes", usageLimitMinutes)
        put("launchLimit", launchLimit)
        put("blockNotifications", blockNotifications)
    }

    companion object {
        const val QUICK_ID = "quick"
        val ALL_DAYS: Set<Int> = (Calendar.SUNDAY..Calendar.SATURDAY).toSet()

        fun hostMatches(host: String, domain: String): Boolean =
            host == domain || host.endsWith(".$domain")

        fun fromJson(o: JSONObject): BlockList = BlockList(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = o.optString("name"),
            enabled = o.optBoolean("enabled", true),
            allowMode = o.optBoolean("allowMode", false),
            apps = o.optJSONArray("apps").toStringSet(),
            websites = o.optJSONArray("websites").toStringSet(),
            keywords = o.optJSONArray("keywords").toStringSet(),
            scheduleEnabled = o.optBoolean("scheduleEnabled", false),
            ranges = readRanges(o),
            usageLimitMinutes = o.optInt("usageLimitMinutes", 0),
            launchLimit = o.optInt("launchLimit", 0),
            blockNotifications = o.optBoolean("blockNotifications", false)
        )

        // New format: "ranges". Old format: one start/end/days -> one range.
        private fun readRanges(o: JSONObject): List<TimeRange> {
            o.optJSONArray("ranges")?.let { arr ->
                val list = (0 until arr.length()).map { TimeRange.fromJson(arr.getJSONObject(it)) }
                if (list.isNotEmpty()) return list
            }
            val days = o.optJSONArray("days")?.let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }.toSet()
            } ?: ALL_DAYS
            return listOf(TimeRange(days, o.optInt("startMinutes", 9 * 60), o.optInt("endMinutes", 17 * 60)))
        }

        private fun JSONArray?.toStringSet(): Set<String> {
            if (this == null) return emptySet()
            return (0 until length()).map { getString(it) }.toSet()
        }
    }
}
