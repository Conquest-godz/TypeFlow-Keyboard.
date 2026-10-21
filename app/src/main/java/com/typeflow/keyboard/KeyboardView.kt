package com.typeflow.keyboard

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardView(context: Context) : LinearLayout(context) {
    private var keyListener: ((String) -> Unit)? = null
    private val keyViews = mutableListOf<KeyView>()
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceRepeating = false
    private var hapticsEnabled = true
    private var keyHeightDp = 48
    private var emojiMode = false
    private var recentEmoji: List<String> = emptyList()
    private var emojiItems: List<String> = emptyList()
    private var emojiAdapter: BaseAdapter? = null
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
        emojiMode = false
        emojiAdapter = null
        emojiItems = emptyList()
        stopBackspaceRepeat()
        removeAllViews()
        keyViews.clear()
        layout.forEach { row ->
            val rowView = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
            row.forEach { label ->
                val key = createKey(label)
                val weight = when {
                    label == "SPACE" -> 4.2f
                    label in listOf("⇧", "⌫", "↵", "?123", "ABC") -> 1.45f
                    label == "😀" -> 1.15f
                    else -> 1f
                }
                key.layoutParams = LayoutParams(0, dp(keyHeightDp), weight).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                }
                when {
                    label == "SPACE" -> installSpaceGesture(key)
                    label == "⌫" -> installBackspaceGesture(key)
                    label.length == 1 && (label[0].isLetter() || KeyboardLayout.alternates.containsKey(label)) -> installLetterGesture(key, label)
                    else -> key.setOnClickListener { keyListener?.invoke(label) }
                }
                keyViews.add(key)
                rowView.addView(key)
            }
            addView(rowView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun createKey(label: String): KeyView = KeyView(context, label).apply {
        hapticsEnabled = this@KeyboardView.hapticsEnabled
        keyHeightDp = this@KeyboardView.keyHeightDp
        contentDescription = when {
            label == "SPACE" -> "Space"
            label == "⌫" -> "Backspace"
            label == "⇧" -> "Shift"
            label == "↵" -> "Enter"
            label == "😀" -> "Open emoji picker"
            label.startsWith("E:") -> label.removePrefix("E:")
            else -> label
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
                    val threshold = (key.width / 8f).coerceAtLeast(dp(12).toFloat())
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
        var downX = 0f
        var deleteWordTriggered = false
        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopBackspaceRepeat()
                    downX = event.rawX
                    deleteWordTriggered = false
                    key.setTouchFeedback(true, performHaptic = true)
                    keyListener?.invoke("⌫")
                    backspaceRepeating = true
                    repeatHandler.postDelayed(backspaceRepeat, BACKSPACE_REPEAT_START_DELAY_MS)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!deleteWordTriggered && downX - event.rawX > touchSlop * 1.5f) {
                        deleteWordTriggered = true
                        stopBackspaceRepeat()
                        keyListener?.invoke("DELETE_WORD")
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    key.setTouchFeedback(false); stopBackspaceRepeat()
                }
            }
            true
        }
    }

    private fun installLetterGesture(key: KeyView, originalLabel: String) {
        var downX = 0f
        var downY = 0f
        var previousX = 0f
        var previousY = 0f
        var swiping = false
        var longPressed = false
        var pendingAlternates: List<String>? = null
        var path = mutableListOf(originalLabel.lowercase()[0])
        val longPress = Runnable {
            val options = KeyboardLayout.alternates[originalLabel.lowercase()]
            if (!options.isNullOrEmpty() && !swiping) {
                longPressed = true
                key.setTouchFeedback(true, performHaptic = true)
                pendingAlternates = options
            }
        }
        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    previousX = downX; previousY = downY
                    swiping = false; longPressed = false; pendingAlternates = null
                    path = mutableListOf(originalLabel.lowercase()[0])
                    key.setTouchFeedback(true, performHaptic = true)
                    repeatHandler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        repeatHandler.removeCallbacks(longPress)
                    }
                    if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop * 1.35) swiping = true
                    if (swiping) sampleSwipePath(previousX, previousY, event.rawX, event.rawY, path)
                    previousX = event.rawX; previousY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    repeatHandler.removeCallbacks(longPress); key.setTouchFeedback(false)
                    when {
                        longPressed -> pendingAlternates?.let { showAlternates(it) }
                        swiping && path.size >= 3 -> {
                            // Only commit a dictionary-recognized word. Inserting the raw path
                            // would turn a missed recognition into visible gibberish.
                            SwipeTypingEngine.recognize(path)?.let { keyListener?.invoke("SWIPEWORD:$it") }
                        }
                        else -> keyListener?.invoke(originalLabel)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    repeatHandler.removeCallbacks(longPress); key.setTouchFeedback(false)
                }
            }
            true
        }
    }

    /** Samples the whole movement segment so fast swipes don't skip keys between MotionEvents. */
    private fun sampleSwipePath(x1: Float, y1: Float, x2: Float, y2: Float, path: MutableList<Char>) {
        val distance = kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        val samples = (distance / dp(8).coerceAtLeast(1)).toInt().coerceIn(1, 64)
        for (i in 1..samples) {
            val fraction = i.toFloat() / samples
            val touched = findLetterAt(x1 + (x2 - x1) * fraction, y1 + (y2 - y1) * fraction)
            if (touched != null && path.lastOrNull() != touched) path.add(touched)
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

    /** One continuous, vertically scrollable Unicode emoji palette; there are no category tabs. */
    private fun showEmojiPicker() {
        emojiMode = true
        stopBackspaceRepeat()
        removeAllViews()
        keyViews.clear()

        // GridView virtualizes emoji cells instead of allocating thousands of TextViews at once.
        // This keeps the entire Unicode palette vertically scrollable without category tabs.
        emojiItems = (recentEmoji + UnicodeEmojiData.all).distinct()
        val grid = GridView(context).apply {
            numColumns = 8
            horizontalSpacing = dp(2)
            verticalSpacing = dp(2)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            contentDescription = "All emoji, scroll vertically"
            adapter = object : BaseAdapter() {
                override fun getCount(): Int = emojiItems.size
                override fun getItem(position: Int): String = emojiItems[position]
                override fun getItemId(position: Int): Long = position.toLong()
                override fun hasStableIds(): Boolean = false

                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val cell = (convertView as? TextView) ?: TextView(context).apply {
                        gravity = Gravity.CENTER
                        textSize = 22f
                        setTextColor(Color.WHITE)
                        isClickable = false
                        isFocusable = false
                        isHapticFeedbackEnabled = false
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = dp(10).toFloat()
                            setColor(Color.rgb(44, 45, 49))
                        }
                        setPadding(dp(1), dp(1), dp(1), dp(1))
                    }
                    val emoji = getItem(position)
                    cell.text = emoji
                    cell.contentDescription = emoji
                    cell.setOnClickListener {
                        if (hapticsEnabled) it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        keyListener?.invoke("E:$emoji")
                    }
                    cell.layoutParams = android.widget.AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, dp(42))
                    return cell
                }
            }
        }
        emojiAdapter = grid.adapter as? BaseAdapter
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, dp(204), 1f))

        val controls = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
        listOf("ABC", "⌫").forEach { label ->
            val key = createKey(label)
            key.layoutParams = LayoutParams(0, dp(keyHeightDp), if (label == "ABC") 2f else 1.4f).apply {
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
            if (label == "⌫") installBackspaceGesture(key)
            else key.setOnClickListener { keyListener?.invoke("ABC") }
            keyViews.add(key)
            controls.addView(key)
        }
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setRecentEmoji(values: List<String>) {
        recentEmoji = values.distinct().take(24)
        if (emojiMode) {
            emojiItems = (recentEmoji + UnicodeEmojiData.all).distinct()
            emojiAdapter?.notifyDataSetChanged()
        }
    }

    fun setPreferences(haptics: Boolean, keyHeight: Int) {
        hapticsEnabled = haptics
        keyHeightDp = keyHeight.coerceIn(40, 64)
        keyViews.forEach { it.hapticsEnabled = hapticsEnabled; it.keyHeightDp = keyHeightDp }
    }

    fun showLetters() = buildLayout(KeyboardLayout.letters)
    fun showNumbers() = buildLayout(KeyboardLayout.numbers)
    fun showEmoji() = showEmojiPicker()
    fun setKeyListener(listener: (String) -> Unit) { keyListener = listener }

    internal fun setShiftState(state: ShiftStateMachine.State) {
        if (emojiMode) return
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
        keyViews.lastOrNull()?.takeIf { it.getLabel() == "↵" || it.getLabel() in listOf("Enter", "Go", "Search", "Send", "Next", "Done", "Previous") }?.let { key ->
            key.setLabel(if (label == "Enter") "↵" else label)
            key.contentDescription = label
        }
    }

    fun stopBackspaceRepeat() {
        backspaceRepeating = false
        repeatHandler.removeCallbacks(backspaceRepeat)
    }

    fun cancelPendingGestures() {
        stopBackspaceRepeat()
        repeatHandler.removeCallbacksAndMessages(null)
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
