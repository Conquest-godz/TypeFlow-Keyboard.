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

    init {
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        isHapticFeedbackEnabled = true
        minimumHeight = dp(44)

        textView.text = displayLabel(label)
        textView.gravity = Gravity.CENTER
        textView.textSize = if (label.startsWith("E:")) 21f else 16f
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.setTextColor(Color.WHITE)
        textView.isClickable = false
        textView.isFocusable = false
        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        refreshBackground(false)
    }

    fun getLabel(): String = label

    fun setLabel(value: String) {
        label = value
        textView.text = displayLabel(value)
    }

    // Touch listeners that consume MotionEvents must call this explicitly;
    // otherwise onTouchEvent() is bypassed and no feedback is produced.
    fun setTouchFeedback(pressed: Boolean, performHaptic: Boolean = false) {
        if (performHaptic) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        refreshBackground(pressed)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> setTouchFeedback(pressed = true, performHaptic = true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setTouchFeedback(pressed = false)
        }
        return super.onTouchEvent(event)
    }

    private fun refreshBackground(isPressed: Boolean) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(if (label.startsWith("E:")) 12 else 10).toFloat()
            setColor(Color.parseColor(if (isPressed) "#555558" else "#2C2C2E"))
        }
        elevation = dp(2).toFloat()
    }

    private fun displayLabel(value: String): String = when {
        value == "SPACE" -> ""
        value.startsWith("E:") -> value.removePrefix("E:")
        else -> value
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
