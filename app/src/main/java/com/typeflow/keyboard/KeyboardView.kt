package com.typeflow.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout

class KeyboardView(context: Context) : LinearLayout(context) {
    private var keyListener: ((String) -> Unit)? = null
    private val keyViews = mutableListOf<KeyView>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        setPadding(dp(4), dp(6), dp(4), dp(24))
        buildLayout(KeyboardLayout.letters)
    }

    private fun buildLayout(layout: List<List<String>>) {
        removeAllViews()
        keyViews.clear()

        layout.forEachIndexed { rowIndex, row ->
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

                val lp = LayoutParams(0, dp(if (rowIndex == 3) 48 else 48), weight)
                lp.setMargins(dp(2), dp(3), dp(2), dp(3))
                key.layoutParams = lp
                key.setOnClickListener { keyListener?.invoke(label) }
                keyViews.add(key)
                rowView.addView(key)
            }

            val rowLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            addView(rowView, rowLp)
        }
    }

    fun showLetters() = buildLayout(KeyboardLayout.letters)
    fun showNumbers() = buildLayout(KeyboardLayout.numbers)

    fun setKeyListener(listener: (String) -> Unit) {
        keyListener = listener
    }

    fun updateEnterKey(label: String) {
        keyViews.lastOrNull()?.setLabel(label)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
