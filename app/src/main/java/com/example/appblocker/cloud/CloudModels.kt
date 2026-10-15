package com.example.appblocker.cloud

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Rows in the shared Supabase project (same account as the desktop app). */

@Serializable
data class ConfigRow(
    val user_id: String? = null,
    val data: JsonElement? = null,
    val updated_by: String? = null,
    val updated_at: String? = null
)

@Serializable
data class DeviceRow(
    val id: String? = null,
    val user_id: String? = null,
    val name: String? = null,
    val platform: String? = null,
    val last_seen: String? = null
)

@Serializable
data class CommandRow(
    val id: String,
    val user_id: String? = null,
    val type: String,
    val target_device: String? = null,
    val consumed: Boolean = false
)
