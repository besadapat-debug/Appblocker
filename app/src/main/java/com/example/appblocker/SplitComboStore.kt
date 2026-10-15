package com.example.appblocker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SplitCombo(
    val id: String = UUID.randomUUID().toString(),
    val topPackage: String,
    val bottomPackage: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("top", topPackage)
        put("bottom", bottomPackage)
    }

    companion object {
        fun fromJson(o: JSONObject) = SplitCombo(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            topPackage = o.getString("top"),
            bottomPackage = o.getString("bottom")
        )
    }
}

// Saved side-by-side app pairs, so a favourite combo can be launched in one tap.
object SplitComboStore {

    private const val PREFS_NAME = "app_blocker_prefs"
    private const val KEY = "split_combos"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(context: Context): List<SplitCombo> {
        val json = prefs(context).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { SplitCombo.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, top: String, bottom: String) {
        val current = getAll(context).toMutableList()
        // Don't store the same pair twice.
        if (current.any { it.topPackage == top && it.bottomPackage == bottom }) return
        current.add(SplitCombo(topPackage = top, bottomPackage = bottom))
        save(context, current)
    }

    fun delete(context: Context, id: String) {
        save(context, getAll(context).filterNot { it.id == id })
    }

    private fun save(context: Context, combos: List<SplitCombo>) {
        val arr = JSONArray()
        combos.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }
}
