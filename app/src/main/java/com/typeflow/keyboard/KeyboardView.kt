package com.typeflow.keyboard

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

class KeyboardView(context: Context) : LinearLayout(context) {
    private var keyListener: ((String) -> Unit)? = null
    private val keyViews = mutableListOf<KeyView>()
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceRepeating = false

    private val backspaceRepeat = object : Runnable {
        override fun run() {
            if (!backspaceRepeating) return
            keyListener?.invoke("⌫")
            repeatHandler.postDelayed(this, BACKSPACE_REPEAT_INTERVAL_MS)
        }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        setPadding(dp(4), dp(6), dp(4), dp(24))
        buildLayout(KeyboardLayout.letters)
    }

    private fun buildLayout(layout: List<List<String>>) {
        stopBackspaceRepeat()
        removeAllViews()
        keyViews.clear()

        layout.forEach { row ->
            val rowView = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
            }

            row.forEach { label ->
                val key = KeyView(context, label)
                key.contentDescription = when (label) {
                    "SPACE" -> "Space"
                    "⌫" -> "Backspace"
                    "⇧" -> "Shift"
                    "↵" -> "Enter"
                    "😀" -> "Emoji"
                    else -> label
                }

                val weight = when (label) {
                    "SPACE" -> 4f
                    "⇧", "⌫", "↵", "?123", "ABC" -> 1.5f
                    else -> 1f
                }

                val lp = LayoutParams(0, dp(48), weight)
                lp.setMargins(dp(2), dp(3), dp(2), dp(3))
                key.layoutParams = lp

                if (label == "⌫") {
                    key.setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                stopBackspaceRepeat()
                                keyListener?.invoke("⌫")
                                backspaceRepeating = true
                                repeatHandler.postDelayed(backspaceRepeat, BACKSPACE_REPEAT_START_DELAY_MS)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopBackspaceRepeat()
                        }
                        true
                    }
                } else {
                    key.setOnClickListener { keyListener?.invoke(key.getLabel()) }
                }

                keyViews.add(key)
                rowView.addView(key)
            }

            addView(rowView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    fun showLetters() = buildLayout(KeyboardLayout.letters)
    fun showNumbers() = buildLayout(KeyboardLayout.numbers)

    fun setKeyListener(listener: (String) -> Unit) {
        keyListener = listener
    }

    fun setShiftState(state: ShiftStateMachine.State) {
        val shifted = state != ShiftStateMachine.State.OFF
        keyViews.forEach { key ->
            val label = key.getLabel()
            if (label.length == 1 && label[0].isLetter()) {
                key.setLabel(if (shifted) label.uppercase() else label.lowercase())
            }
            if (label == "⇧") {
                key.isSelected = state != ShiftStateMachine.State.OFF
                key.contentDescription = when (state) {
                    ShiftStateMachine.State.OFF -> "Shift off"
                    ShiftStateMachine.State.SHIFT -> "Shift on"
                    ShiftStateMachine.State.CAPS_LOCK -> "Caps Lock on"
                }
            }
        }
    }

    fun updateEnterKey(label: String) {
        keyViews.lastOrNull()?.let { key ->
            key.setLabel(when (label) {
                "Enter" -> "↵"
                else -> label
            })
            key.contentDescription = label
        }
    }

    fun stopBackspaceRepeat() {
        backspaceRepeating = false
        repeatHandler.removeCallbacks(backspaceRepeat)
    }

    override fun onDetachedFromWindow() {
        stopBackspaceRepeat()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val BACKSPACE_REPEAT_START_DELAY_MS = 400L
        const val BACKSPACE_REPEAT_INTERVAL_MS = 50L
    }
}
