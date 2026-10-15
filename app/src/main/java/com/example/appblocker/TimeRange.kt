package com.example.appblocker

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

// One "block during these days, between these times" rule. A list can have
// several (e.g. weekdays 9am-5pm AND Sunday 7pm-10pm).
// A range that crosses midnight (10pm-7am) belongs to the day it STARTS on.
// Start == end means all day.
data class TimeRange(
    val days: Set<Int> = WEEKDAYS,
    val startMinutes: Int = 9 * 60,
    val endMinutes: Int = 17 * 60
) {

    fun contains(now: Calendar): Boolean {
        if (days.isEmpty()) return false
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = now.get(Calendar.DAY_OF_WEEK)
        val yesterday = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1
        return when {
            startMinutes == endMinutes -> today in days
            startMinutes < endMinutes -> today in days && nowMinutes >= startMinutes && nowMinutes < endMinutes
            else -> (today in days && nowMinutes >= startMinutes) || (yesterday in days && nowMinutes < endMinutes)
        }
    }

    // When this range stops covering `now` (null if it doesn't cover now or never ends).
    fun endMillis(now: Calendar): Long? {
        if (!contains(now)) return null
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return when {
            startMinutes == endMinutes -> {
                var offset = 1
                while (offset < 8) {
                    val c = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                    if (c.get(Calendar.DAY_OF_WEEK) !in days) break
                    offset++
                }
                if (offset >= 8) null else at(now, offset, 0)
            }
            startMinutes < endMinutes -> at(now, 0, endMinutes)
            nowMinutes >= startMinutes -> at(now, 1, endMinutes)
            else -> at(now, 0, endMinutes)
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("days", JSONArray(days.toList()))
        put("start", startMinutes)
        put("end", endMinutes)
    }

    companion object {
        val WEEKDAYS: Set<Int> = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
        )

        fun fromJson(o: JSONObject): TimeRange = TimeRange(
            days = o.optJSONArray("days")?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: WEEKDAYS,
            startMinutes = o.optInt("start", 9 * 60),
            endMinutes = o.optInt("end", 17 * 60)
        )

        private fun at(now: Calendar, dayOffset: Int, minutes: Int): Long = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
