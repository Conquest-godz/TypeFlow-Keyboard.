package com.typeflow.keyboard

import android.content.Context

/** Local preferences only; no typed text is recorded here. */
class KeyboardPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("typeflow_settings", Context.MODE_PRIVATE)

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) { prefs.edit().putBoolean(KEY_HAPTICS, value).apply() }

    var keyHeightDp: Int
        get() = prefs.getInt(KEY_HEIGHT, 48).coerceIn(40, 64)
        set(value) { prefs.edit().putInt(KEY_HEIGHT, value.coerceIn(40, 64)).apply() }

    var themePreset: String
        get() = prefs.getString("theme_preset", "midnight") ?: "midnight"
        set(value) { prefs.edit().putString("theme_preset", value).apply() }

    var keyCornerDp: Int
        get() = prefs.getInt("key_corner_dp", 10).coerceIn(2, 20)
        set(value) { prefs.edit().putInt("key_corner_dp", value.coerceIn(2, 20)).apply() }

    fun recentEmoji(): List<String> = prefs.getString(KEY_RECENT_EMOJI, null)
        ?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()

    fun rememberEmoji(emoji: String) {
        val updated = (listOf(emoji) + recentEmoji().filterNot { it == emoji }).take(24)
        prefs.edit().putString(KEY_RECENT_EMOJI, updated.joinToString(SEP)).apply()
    }

    companion object {
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_HEIGHT = "key_height_dp"
        private const val KEY_RECENT_EMOJI = "recent_emoji"
        private const val SEP = "\u001f"
    }
}
