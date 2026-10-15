package com.example.appblocker

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ScheduleFormat {

    val WEEK_ORDER = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    val SHORT_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
    private val SHORT_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private val WEEKDAYS = WEEK_ORDER.take(5).toSet()
    private val WEEKEND = setOf(Calendar.SATURDAY, Calendar.SUNDAY)

    val USAGE_LIMIT_CHOICES = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240)
    val LAUNCH_LIMIT_CHOICES = listOf(1, 2, 3, 5, 10, 15, 20, 30, 50)

    fun time(totalMinutes: Int): String {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, totalMinutes / 60)
        c.set(Calendar.MINUTE, totalMinutes % 60)
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.time).lowercase(Locale.getDefault())
    }

    fun clock(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())

    // "at 5:00 pm", "at midnight", "tomorrow at 7:00 am", "Tue at 9:00 am"
    fun moment(millis: Long, now: Long): String {
        val t = Calendar.getInstance().apply { timeInMillis = millis }
        val n = Calendar.getInstance().apply { timeInMillis = now }
        val isMidnight = t.get(Calendar.HOUR_OF_DAY) == 0 && t.get(Calendar.MINUTE) == 0
        val dayDiff = daysBetween(n, t)
        return when {
            isMidnight && dayDiff == 1 -> "at midnight"
            dayDiff == 0 -> "at ${clock(millis)}"
            dayDiff == 1 -> "tomorrow at ${clock(millis)}"
            else -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis)) + " at ${clock(millis)}"
        }
    }

    private fun daysBetween(a: Calendar, b: Calendar): Int {
        val a0 = (a.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 12) }
        val b0 = (b.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 12) }
        return ((b0.timeInMillis - a0.timeInMillis) / 86_400_000L).toInt()
    }

    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    fun durationMs(ms: Long): String {
        val totalMin = ms / 60_000L
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun days(days: Set<Int>): String = when {
        days.size == 7 -> "Every day"
        days.isEmpty() -> "No days selected"
        days == WEEKDAYS -> "Weekdays"
        days == WEEKEND -> "Weekends"
        else -> WEEK_ORDER.mapIndexedNotNull { i, d -> if (d in days) SHORT_NAMES[i] else null }
            .joinToString(", ")
    }

    fun limits(list: BlockList): String? {
        val parts = mutableListOf<String>()
        if (list.usageLimitMinutes > 0) parts.add("${duration(list.usageLimitMinutes)} a day")
        if (list.launchLimit > 0) parts.add("${list.launchLimit} opens a day")
        return if (parts.isEmpty()) null else "after " + parts.joinToString(" or ")
    }

    fun range(r: TimeRange): String {
        val hours = if (r.startMinutes == r.endMinutes) "all day"
        else "${time(r.startMinutes)} – ${time(r.endMinutes)}"
        return "${days(r.days)} · $hours"
    }

    fun summary(list: BlockList): String {
        val limits = limits(list)
        if (!list.scheduleEnabled) {
            return limits?.replaceFirstChar { it.uppercase() } ?: "All the time"
        }
        val first = list.ranges.firstOrNull()?.let { range(it) } ?: "No times set"
        val more = if (list.ranges.size > 1) " (+${list.ranges.size - 1} more)" else ""
        return first + more + (limits?.let { " · $it" } ?: "")
    }

    fun contents(list: BlockList): String {
        val parts = mutableListOf<String>()
        fun plural(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"
        if (list.allowMode) {
            parts.add("Everything except ${plural(list.apps.size, "app")}")
            if (list.websites.isNotEmpty()) parts.add("${plural(list.websites.size, "site")} allowed")
        } else {
            parts.add(plural(list.apps.size, "app"))
            parts.add(plural(list.websites.size, "website"))
        }
        if (list.keywords.isNotEmpty()) parts.add(plural(list.keywords.size, "keyword"))
        if (list.blockNotifications) parts.add("notifications off")
        return parts.joinToString(" · ")
    }
}
