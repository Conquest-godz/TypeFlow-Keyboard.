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
        minimumHeight = dp(44)

        textView.text = displayLabel(label)
        textView.gravity = Gravity.CENTER
        textView.textSize = 16f
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.setTextColor(Color.WHITE)
        addView(textView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        refreshBackground(false)
    }

    fun getLabel(): String = label

    fun setLabel(value: String) {
        label = value
        textView.text = displayLabel(value)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                refreshBackground(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> refreshBackground(false)
        }
        return super.onTouchEvent(event)
    }

    private fun refreshBackground(pressed: Boolean) {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor(if (pressed) "#48484A" else "#2C2C2E"))
        }
        elevation = dp(2).toFloat()
    }

    private fun displayLabel(value: String): String = when (value) {
        "SPACE" -> ""
        "?123" -> "?123"
        else -> value
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
