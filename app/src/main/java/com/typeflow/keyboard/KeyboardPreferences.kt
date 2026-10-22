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

    /** Theme tokens used by the first TypeFlow Studio controls. */
    var keyColor: String
        get() = prefs.getString(KEY_COLOR, COLOR_GRAPHITE) ?: COLOR_GRAPHITE
        set(value) { prefs.edit().putString(KEY_COLOR, value.takeIf { it in KEY_COLORS } ?: COLOR_GRAPHITE).apply() }

    var keyShape: String
        get() = prefs.getString(KEY_SHAPE, SHAPE_ROUNDED) ?: SHAPE_ROUNDED
        set(value) { prefs.edit().putString(KEY_SHAPE, value.takeIf { it in KEY_SHAPES } ?: SHAPE_ROUNDED).apply() }

    var pressEffectEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRESS_EFFECT, true)
        set(value) { prefs.edit().putBoolean(KEY_PRESS_EFFECT, value).apply() }

    companion object {
        const val COLOR_GRAPHITE = "#2C2D31"
        const val COLOR_MIDNIGHT = "#17191F"
        const val COLOR_OCEAN = "#244B67"
        const val COLOR_VIOLET = "#49345F"
        const val SHAPE_ROUNDED = "rounded"
        const val SHAPE_PILL = "pill"
        const val SHAPE_SQUARE = "square"
        val KEY_COLORS = setOf(COLOR_GRAPHITE, COLOR_MIDNIGHT, COLOR_OCEAN, COLOR_VIOLET)
        val KEY_SHAPES = setOf(SHAPE_ROUNDED, SHAPE_PILL, SHAPE_SQUARE)
    }

    fun recentEmoji(): List<String> = prefs.getString(KEY_RECENT_EMOJI, null)
        ?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()

    fun rememberEmoji(emoji: String) {
        val updated = (listOf(emoji) + recentEmoji().filterNot { it == emoji }).take(24)
        prefs.edit().putString(KEY_RECENT_EMOJI, updated.joinToString(SEP)).apply()
    }

    companion object {
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_HEIGHT = "key_height_dp"
        private const val KEY_COLOR = "studio_key_color"
        private const val KEY_SHAPE = "studio_key_shape"
        private const val KEY_PRESS_EFFECT = "studio_press_effect"
        private const val KEY_RECENT_EMOJI = "recent_emoji"
        private const val SEP = "\u001f"
    }
}
