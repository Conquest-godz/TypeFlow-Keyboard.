package com.typeflow.keyboard

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout

class KeyboardView(context: Context) : LinearLayout(context) {
    private var keyListener: ((String) -> Unit)? = null
    private val keyViews = mutableListOf<KeyView>()
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceRepeating = false
    private var hapticsEnabled = true
    private var keyHeightDp = 48
    private var emojiCategory = "Smileys"
    private var recentEmoji: List<String> = emptyList()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
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
        setBackgroundColor(Color.rgb(25, 26, 29))
        setPadding(dp(4), dp(6), dp(4), dp(18))
        buildLayout(KeyboardLayout.letters)
    }

    private fun buildLayout(layout: List<List<String>>) {
        stopBackspaceRepeat()
        removeAllViews()
        keyViews.clear()
        layout.forEach { row ->
            val rowView = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
            row.forEach { label ->
                val key = KeyView(context, label).apply {
                    hapticsEnabled = this@KeyboardView.hapticsEnabled
                    keyHeightDp = this@KeyboardView.keyHeightDp
                    contentDescription = when {
                        label == "SPACE" -> "Space"
                        label == "⌫" -> "Backspace"
                        label == "⇧" -> "Shift"
                        label == "↵" -> "Enter"
                        label == "😀" -> "Open emoji picker"
                        label.startsWith("E:") -> label.removePrefix("E:")
                        label.startsWith("CAT:") -> "Emoji category " + label.removePrefix("CAT:")
                        else -> label
                    }
                }
                val weight = when {
                    label == "SPACE" -> 4.2f
                    label in listOf("⇧", "⌫", "↵", "?123", "ABC") -> 1.45f
                    label == "😀" -> 1.15f
                    label.startsWith("CAT:") -> 0.9f
                    else -> 1f
                }
                key.layoutParams = LayoutParams(0, dp(keyHeightDp), weight).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                }
                when {
                    label == "SPACE" -> installSpaceGesture(key)
                    label == "⌫" -> installBackspaceGesture(key)
                    label.length == 1 && label[0].isLetter() -> installLetterGesture(key, label)
                    label.startsWith("CAT:") -> key.setOnClickListener {
                        emojiCategory = label.removePrefix("CAT:")
                        showEmojiCategory(emojiCategory)
                    }
                    else -> key.setOnClickListener { keyListener?.invoke(label) }
                }
                keyViews.add(key)
                rowView.addView(key)
            }
            addView(rowView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun installSpaceGesture(key: KeyView) {
        var previousX = 0f
        var accumulatedX = 0f
        var moved = false
        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = event.rawX; accumulatedX = 0f; moved = false
                    key.setTouchFeedback(true, performHaptic = true)
                }
                MotionEvent.ACTION_MOVE -> {
                    accumulatedX += event.rawX - previousX
                    previousX = event.rawX
                    if (kotlin.math.abs(accumulatedX) > touchSlop) moved = true
                    val threshold = (key.width / 2f).coerceAtLeast(1f)
                    val steps = GestureMath.cursorSteps(accumulatedX, threshold)
                    if (steps != 0) {
                        keyListener?.invoke("CURSOR:$steps")
                        accumulatedX -= steps * threshold
                    }
                }
                MotionEvent.ACTION_UP -> {
                    key.setTouchFeedback(false)
                    if (!moved) keyListener?.invoke("SPACE")
                }
                MotionEvent.ACTION_CANCEL -> key.setTouchFeedback(false)
            }
            true
        }
    }

    private fun installBackspaceGesture(key: KeyView) {
        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopBackspaceRepeat()
                    key.setTouchFeedback(true, performHaptic = true)
                    keyListener?.invoke("⌫")
                    backspaceRepeating = true
                    repeatHandler.postDelayed(backspaceRepeat, BACKSPACE_REPEAT_START_DELAY_MS)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    key.setTouchFeedback(false); stopBackspaceRepeat()
                }
            }
            true
        }
    }

    private fun installLetterGesture(key: KeyView, label: String) {
        var downX = 0f
        var downY = 0f
        var swiping = false
        var longPressed = false
        var path = mutableListOf<Char>()
        val longPress = Runnable {
            val options = KeyboardLayout.alternates[label.lowercase()]
            if (!options.isNullOrEmpty()) {
                longPressed = true
                key.setTouchFeedback(true, performHaptic = true)
                showAlternates(options)
            }
        }
        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    swiping = false; longPressed = false
                    path = mutableListOf(label.lowercase()[0])
                    key.setTouchFeedback(true, performHaptic = true)
                    repeatHandler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(event.rawX - downX) > touchSlop ||
                        kotlin.math.abs(event.rawY - downY) > touchSlop) repeatHandler.removeCallbacks(longPress)
                    if (kotlin.math.abs(event.rawX - downX) > touchSlop * 1.4f) swiping = true
                    if (swiping) {
                        val touched = findLetterAt(event.rawX, event.rawY)
                        if (touched != null && path.lastOrNull() != touched) path.add(touched)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    repeatHandler.removeCallbacks(longPress); key.setTouchFeedback(false)
                    when {
                        longPressed -> Unit
                        swiping && path.size >= 3 -> {
                            val word = SwipeTypingEngine.recognize(path)
                            if (word != null) keyListener?.invoke("SWIPEWORD:$word") else keyListener?.invoke(label)
                        }
                        else -> keyListener?.invoke(label)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    repeatHandler.removeCallbacks(longPress); key.setTouchFeedback(false)
                    if (longPressed) showLetters()
                }
            }
            true
        }
    }

    private fun findLetterAt(rawX: Float, rawY: Float): Char? =
        keyViews.firstOrNull { key ->
            val label = key.getLabel()
            if (label.length != 1 || !label[0].isLetter()) return@firstOrNull false
            val location = IntArray(2); key.getLocationOnScreen(location)
            rawX >= location[0] && rawX <= location[0] + key.width &&
                rawY >= location[1] && rawY <= location[1] + key.height
        }?.getLabel()?.firstOrNull()?.lowercaseChar()

    private fun showAlternates(options: List<String>) {
        buildLayout(listOf(options.map { "ALT:$it" } + "ALT:CANCEL"))
        keyViews.forEach { key ->
            key.setOnClickListener {
                val value = key.getLabel()
                if (value != "ALT:CANCEL") keyListener?.invoke(value.removePrefix("ALT:"))
                showLetters()
            }
        }
    }

    private fun showEmojiCategory(category: String) {
        val values = if (category == "Recent") recentEmoji.ifEmpty {
            KeyboardLayout.emojiCategories["Recent"].orEmpty()
        } else KeyboardLayout.emojiCategories[category] ?: KeyboardLayout.emojiCategories["Smileys"]!!
        val rows = values.map { "E:$it" }.chunked(8).toMutableList()
        rows.add(listOf("ABC", "CAT:Recent", "CAT:Smileys", "CAT:Gestures", "CAT:Hearts", "CAT:Symbols", "⌫"))
        buildLayout(rows)
    }

    fun setRecentEmoji(values: List<String>) {
        recentEmoji = values
        if (emojiCategory == "Recent") showEmojiCategory("Recent")
    }

    fun setPreferences(haptics: Boolean, keyHeight: Int) {
        hapticsEnabled = haptics
        keyHeightDp = keyHeight.coerceIn(40, 64)
        keyViews.forEach { it.hapticsEnabled = hapticsEnabled; it.keyHeightDp = keyHeightDp }
    }

    fun showLetters() = buildLayout(KeyboardLayout.letters)
    fun showNumbers() = buildLayout(KeyboardLayout.numbers)
    fun showEmoji() = showEmojiCategory(emojiCategory)
    fun setKeyListener(listener: (String) -> Unit) { keyListener = listener }

    internal fun setShiftState(state: ShiftStateMachine.State) {
        val shifted = state != ShiftStateMachine.State.OFF
        keyViews.forEach { key ->
            val label = key.getLabel()
            if (label.length == 1 && label[0].isLetter()) key.setLabel(if (shifted) label.uppercase() else label.lowercase())
            if (label == "⇧") {
                key.setShiftActive(shifted)
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
            key.setLabel(if (label == "Enter") "↵" else label)
            key.contentDescription = label
        }
    }

    fun stopBackspaceRepeat() {
        backspaceRepeating = false
        repeatHandler.removeCallbacks(backspaceRepeat)
    }

    override fun onDetachedFromWindow() {
        stopBackspaceRepeat()
        repeatHandler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private companion object {
        const val BACKSPACE_REPEAT_START_DELAY_MS = 400L
        const val BACKSPACE_REPEAT_INTERVAL_MS = 50L
    }
}
