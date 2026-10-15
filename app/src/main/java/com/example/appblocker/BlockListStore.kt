package com.example.appblocker

import android.content.Context
import com.example.appblocker.cloud.CloudSync
import org.json.JSONArray

// All block lists (including the special Quick Block list) as one JSON
// string, cached in memory because the blocker reads it constantly.
object BlockListStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY_LISTS = "block_lists_json"

    private const val LEGACY_BLOCKED_PACKAGES = "blocked_packages"
    private const val LEGACY_SCHEDULE_ENABLED = "schedule_enabled"
    private const val LEGACY_START_MINUTES = "schedule_start_minutes"
    private const val LEGACY_END_MINUTES = "schedule_end_minutes"

    @Volatile
    private var cache: List<BlockList>? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): List<BlockList> {
        cache?.let { return it }
        val json = prefs(context).getString(KEY_LISTS, null)
        val lists = if (json == null) {
            migrateLegacy(context)
        } else {
            try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { BlockList.fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        }
        cache = lists
        return lists
    }

    // Lists shown under "Block lists" (everything except Quick Block).
    fun userLists(context: Context): List<BlockList> = getAll(context).filterNot { it.isQuick }

    fun get(context: Context, id: String): BlockList? = getAll(context).firstOrNull { it.id == id }

    fun quickList(context: Context): BlockList =
        get(context, BlockList.QUICK_ID) ?: BlockList(id = BlockList.QUICK_ID, name = "Quick Block")

    @Synchronized
    fun upsert(context: Context, list: BlockList) {
        val current = getAll(context).toMutableList()
        val index = current.indexOfFirst { it.id == list.id }
        if (index >= 0) current[index] = list else current.add(list)
        saveAll(context, current)
    }

    @Synchronized
    fun delete(context: Context, id: String) {
        saveAll(context, getAll(context).filterNot { it.id == id })
    }

    fun setEnabled(context: Context, id: String, enabled: Boolean) {
        val list = get(context, id) ?: return
        upsert(context, list.copy(enabled = enabled))
    }

    private fun saveAll(context: Context, lists: List<BlockList>) {
        val arr = JSONArray()
        lists.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_LISTS, arr.toString()).apply()
        cache = lists
        // Sync up to any other signed-in devices (no-op when not signed in).
        CloudSync.pushLists()
    }

    // ---- Cloud sync helpers ----

    /** The raw block-lists JSON array string (what gets synced). */
    fun rawJson(context: Context): String {
        val arr = JSONArray()
        getAll(context).forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    /** Apply block lists pulled from the cloud. Does NOT re-push (avoids loops). */
    @Synchronized
    fun applyRemoteJson(context: Context, jsonArray: String) {
        val lists = try {
            val arr = JSONArray(jsonArray)
            (0 until arr.length()).map { BlockList.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { return }
        prefs(context).edit().putString(KEY_LISTS, JSONArray(jsonArray).toString()).apply()
        cache = lists
    }

    /** Turn every block list on (used by the remote "Lock all" command). */
    @Synchronized
    fun enableAll(context: Context) {
        saveAll(context, getAll(context).map { if (it.enabled) it else it.copy(enabled = true) })
    }

    // Old "one app list + one schedule" -> first block list.
    private fun migrateLegacy(context: Context): List<BlockList> {
        val prefs = prefs(context)
        val legacyApps = prefs.getStringSet(LEGACY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
        val lists = if (legacyApps.isEmpty()) emptyList() else listOf(
            BlockList(
                name = "My block list",
                apps = legacyApps.toSet(),
                scheduleEnabled = prefs.getBoolean(LEGACY_SCHEDULE_ENABLED, false),
                ranges = listOf(
                    TimeRange(
                        days = BlockList.ALL_DAYS,
                        startMinutes = prefs.getInt(LEGACY_START_MINUTES, 9 * 60),
                        endMinutes = prefs.getInt(LEGACY_END_MINUTES, 17 * 60)
                    )
                )
            )
        )
        val arr = JSONArray()
        lists.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_LISTS, arr.toString()).apply()
        return lists
    }
}
