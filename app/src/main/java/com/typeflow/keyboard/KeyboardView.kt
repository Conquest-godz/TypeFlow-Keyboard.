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
                key.contentDescription = when {
                    label == "SPACE" -> "Space"
                    label == "⌫" -> "Backspace"
                    label == "⇧" -> "Shift"
                    label == "↵" -> "Enter"
                    label == "😀" -> "Open emoji picker"
                    label.startsWith("E:") -> label.removePrefix("E:")
                    else -> label
                }

                val weight = when {
                    label == "SPACE" -> 4f
                    label in listOf("⇧", "⌫", "↵", "?123", "ABC") -> 1.5f
                    label == "😀" -> 1.2f
                    else -> 1f
                }

                val lp = LayoutParams(0, dp(if (layout == KeyboardLayout.emoji) 43 else 48), weight)
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
                    key.setOnClickListener { keyListener?.invoke(label) }
                }

                keyViews.add(key)
                rowView.addView(key)
            }

            addView(rowView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    fun showLetters() = buildLayout(KeyboardLayout.letters)
    fun showNumbers() = buildLayout(KeyboardLayout.numbers)
    fun showEmoji() = buildLayout(KeyboardLayout.emoji)

    fun setKeyListener(listener: (String) -> Unit) {
        keyListener = listener
    }

    fun updateEnterKey(label: String) {
        keyViews.lastOrNull()?.setLabel(label)
    }

    override fun onDetachedFromWindow() {
        stopBackspaceRepeat()
        // Keep the listener: InputMethodService may detach and reuse this view.
        super.onDetachedFromWindow()
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeating = false
        repeatHandler.removeCallbacks(backspaceRepeat)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val BACKSPACE_REPEAT_START_DELAY_MS = 400L
        const val BACKSPACE_REPEAT_INTERVAL_MS = 50L
    }
}
