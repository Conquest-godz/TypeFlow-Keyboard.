package com.typeflow.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView

class KeyView(context: Context, private var label: String) : LinearLayout(context) {
    private val textView = TextView(context)
    private var pressed = false
    private var shiftActive = false
    var hapticsEnabled: Boolean = true
        set(value) {
            field = value
            isHapticFeedbackEnabled = value
        }
    var keyHeightDp: Int = 48
        set(value) { field = value.coerceIn(40, 64); minimumHeight = dp(field) }

    init {
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        isHapticFeedbackEnabled = true
        minimumHeight = dp(keyHeightDp)
        textView.text = displayLabel(label)
        textView.gravity = Gravity.CENTER
        textView.textSize = if (label.startsWith("E:")) 22f else 16f
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.setTextColor(Color.WHITE)
        textView.isClickable = false
        textView.isFocusable = false
        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        refreshBackground()
    }

    fun getLabel(): String = label
    fun setLabel(value: String) {
        label = value
        textView.text = displayLabel(value)
        textView.textSize = if (value.startsWith("E:")) 22f else 16f
        refreshBackground()
    }
    fun setShiftActive(active: Boolean) { shiftActive = active; refreshBackground() }

    fun setTouchFeedback(pressed: Boolean, performHaptic: Boolean = false) {
        if (performHaptic && hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        this.pressed = pressed
        refreshBackground()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> setTouchFeedback(true, performHaptic = true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setTouchFeedback(false)
        }
        return super.onTouchEvent(event)
    }

    private fun refreshBackground() {
        val color = when { pressed -> "#55565A"; shiftActive -> "#66686D"; else -> "#2C2D31" }
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(if (label.startsWith("E:")) 12 else 10).toFloat()
            setColor(Color.parseColor(color))
        }
        elevation = dp(2).toFloat()
    }

    private fun displayLabel(value: String): String = when {
        value == "SPACE" -> ""
        value.startsWith("E:") -> value.removePrefix("E:")
        value.startsWith("CAT:") -> when (value.removePrefix("CAT:")) {
            "Recent" -> "◷"; "Smileys" -> "☺"; "Gestures" -> "☝"; "Hearts" -> "♥"; "Symbols" -> "✦"; else -> value
        }
        value.startsWith("ALT:") -> if (value == "ALT:CANCEL") "×" else value.removePrefix("ALT:")
        else -> value
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
