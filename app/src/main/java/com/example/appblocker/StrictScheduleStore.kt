package com.example.appblocker

import android.content.Context
import org.json.JSONArray
import java.util.Calendar

// Weekly schedule governing the "Follow schedules" unlock method. Scoped so the
// master lock and each per-list / adult lock can have their own locked windows.
object StrictScheduleStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY = "strict_unlock_ranges"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(scope: String) = KEY + scope

    fun getRanges(context: Context, scope: String = ""): List<TimeRange> {
        val json = prefs(context).getString(key(scope), null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { TimeRange.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setRanges(context: Context, ranges: List<TimeRange>, scope: String = "") {
        val arr = JSONArray()
        ranges.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(key(scope), arr.toString()).apply()
    }

    fun hasSchedule(context: Context, scope: String = ""): Boolean = getRanges(context, scope).isNotEmpty()

    // True while now falls inside any range (lock stays on).
    fun isWithinSchedule(context: Context, now: Calendar = Calendar.getInstance(), scope: String = ""): Boolean =
        getRanges(context, scope).any { it.contains(now) }
}
