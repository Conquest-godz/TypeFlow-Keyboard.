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
import android.widget.PopupWindow
import android.widget.TextView

class KeyboardView(context: Context) : LinearLayout(context) {
    private var keyListener: ((String) -> Unit)? = null
    private val keyViews = mutableListOf<KeyView>()
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var backspaceRepeating = false
    private var backspaceRepeatCount = 0
    private var backspaceDidDelete = false
    private var hapticsEnabled = true
    private var keyHeightDp = 48
    private var keyColor = KeyboardPreferences.COLOR_GRAPHITE
    private var keyShape = KeyboardPreferences.SHAPE_ROUNDED
    private var pressEffectEnabled = true
    private var emojiMode = false
    private var recentEmoji: List<String> = emptyList()
    private var emojiItems: List<String> = emptyList()
    private var emojiAdapter: BaseAdapter? = null
    private var emojiGrid: GridView? = null
    private var emojiCategory = 0
    private var regularKeyboardHeightPx = 0
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val backspaceRepeat = object : Runnable {
        override fun run() {
            if (!backspaceRepeating) return
            keyListener?.invoke("⌫")
            backspaceDidDelete = true
            backspaceRepeatCount++
            // Accelerate gently while held, then keep a steady continuous repeat cadence.
            val interval = if (backspaceRepeatCount >= BACKSPACE_ACCELERATION_COUNT) {
                BACKSPACE_REPEAT_FAST_INTERVAL_MS
            } else {
                BACKSPACE_REPEAT_INTERVAL_MS
            }
            repeatHandler.postDelayed(this, interval)
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
        emojiGrid = null
        emojiCategory = 0
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
        keyColor = this@KeyboardView.keyColor
        keyShape = this@KeyboardView.keyShape
        pressEffectEnabled = this@KeyboardView.pressEffectEnabled
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
        var totalTravelX = 0f
        var moved = false
        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = event.rawX; accumulatedX = 0f; totalTravelX = 0f; moved = false
                    key.setTouchFeedback(true, performHaptic = true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = event.rawX - previousX
                    accumulatedX += delta
                    totalTravelX += kotlin.math.abs(delta)
                    previousX = event.rawX
                    if (kotlin.math.abs(totalTravelX) > touchSlop) moved = true
                    // Gboard-like cursor scrubbing should respond to a short, deliberate slide;
                    // tying the threshold to the whole spacebar made it unnecessarily sluggish.
                    val threshold = dp(CURSOR_STEP_DP).coerceAtLeast(1).toFloat()
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
        var previewActive = false
        var lastPreviewCount = 0
        key.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopBackspaceRepeat()
                    backspaceRepeatCount = 0
                    backspaceDidDelete = false
                    downX = event.rawX
                    previewActive = false
                    lastPreviewCount = 0
                    key.setTouchFeedback(true, performHaptic = true)
                    backspaceRepeating = true
                    repeatHandler.postDelayed(backspaceRepeat, BACKSPACE_REPEAT_START_DELAY_MS)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dragDp = ((downX - event.rawX) / resources.displayMetrics.density).coerceAtLeast(0f)
                    if (dragDp >= DELETE_PREVIEW_START_DP) {
                        if (!previewActive) {
                            previewActive = true
                            stopBackspaceRepeat()
                        }
                        val count = ((dragDp - DELETE_PREVIEW_START_DP) / DELETE_PREVIEW_CHAR_DP).toInt() + 1
                        if (count != lastPreviewCount) {
                            lastPreviewCount = count
                            keyListener?.invoke("DELETE_PREVIEW:$count")
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    key.setTouchFeedback(false)
                    if (previewActive) keyListener?.invoke("DELETE_PREVIEW_COMMIT")
                    else if (!backspaceDidDelete) {
                        keyListener?.invoke("⌫")
                    }
                    stopBackspaceRepeat()
                }
                MotionEvent.ACTION_CANCEL -> {
                    key.setTouchFeedback(false)
                    if (previewActive) keyListener?.invoke("DELETE_PREVIEW_CANCEL")
                    stopBackspaceRepeat()
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
        var gesturePoints = mutableListOf<SwipeTypingEngine.Point>()
        var swipeLetterBounds: List<Pair<android.graphics.Rect, Char>> = emptyList()
        var previewPopup: PopupWindow? = null
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
                    gesturePoints = mutableListOf(normalizedGesturePoint(event.rawX, event.rawY))
                    swipeLetterBounds = captureLetterBounds()
                    key.setTouchFeedback(true, performHaptic = true)
                    previewPopup?.dismiss()
                    previewPopup = showLetterPreview(key, originalLabel)
                    repeatHandler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        repeatHandler.removeCallbacks(longPress)
                        previewPopup?.dismiss()
                        previewPopup = null
                    }
                    if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop * 1.35) swiping = true
                    if (swiping) {
                        sampleSwipePath(previousX, previousY, event.rawX, event.rawY, path, swipeLetterBounds)
                        appendGesturePoint(event.rawX, event.rawY, gesturePoints)
                    }
                    previousX = event.rawX; previousY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    repeatHandler.removeCallbacks(longPress); key.setTouchFeedback(false)
                    previewPopup?.dismiss(); previewPopup = null
                    if (swiping) {
                        sampleSwipePath(previousX, previousY, event.rawX, event.rawY, path, swipeLetterBounds)
                        appendGesturePoint(event.rawX, event.rawY, gesturePoints)
                    }
                    when {
                        longPressed -> pendingAlternates?.let { showAlternates(it) }
                        swiping -> {
                            // Only commit a dictionary-recognized word. Inserting the raw path
                            // would turn a missed recognition into visible gibberish.
                            if (gesturePoints.size >= 2 && path.size >= 2) {
                                // Prefer geometric decoding. If its confidence gate rejects the
                                // trace, fall back to the offline lexical path decoder so ordinary
                                // swipes don't silently do nothing. This remains local-only.
                                val centers = keyCenterMap()
                                val decoded = SwipeTypingEngine.recognizeSpatial(path, gesturePoints, centers)
                                    ?: SwipeTypingEngine.recognize(path)
                                decoded?.let { keyListener?.invoke("SWIPEWORD:$it") }
                            }
                        }
                        !swiping -> keyListener?.invoke(originalLabel)
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    repeatHandler.removeCallbacks(longPress); key.setTouchFeedback(false)
                    previewPopup?.dismiss(); previewPopup = null
                }
            }
            true
        }
    }

    private fun showLetterPreview(key: KeyView, label: String): PopupWindow {
        val bubble = TextView(context).apply {
            text = label
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.rgb(66, 68, 74))
                setStroke(dp(1), Color.rgb(110, 112, 120))
            }
            elevation = dp(5).toFloat()
        }
        val popup = PopupWindow(bubble, dp(48), dp(58), false).apply {
            isTouchable = false
            isFocusable = false
            isOutsideTouchable = false
            elevation = dp(5).toFloat()
        }
        // Anchor to the actual key view. Absolute screen coordinates are unreliable for IME
        // windows on some Android versions and can place previews at the bottom of the keyboard.
        popup.showAsDropDown(key, (key.width - dp(48)) / 2, -key.height - dp(58), Gravity.START)
        return popup
    }

    private fun normalizedGesturePoint(rawX: Float, rawY: Float): SwipeTypingEngine.Point {
        val origin = IntArray(2)
        getLocationOnScreen(origin)
        return SwipeTypingEngine.Point(
            ((rawX - origin[0]) / width.coerceAtLeast(1)).coerceIn(0f, 1f),
            ((rawY - origin[1]) / height.coerceAtLeast(1)).coerceIn(0f, 1f)
        )
    }

    private fun appendGesturePoint(rawX: Float, rawY: Float, points: MutableList<SwipeTypingEngine.Point>) {
        val point = normalizedGesturePoint(rawX, rawY)
        val last = points.lastOrNull()
        if (last == null || kotlin.math.hypot((point.x - last.x).toDouble(), (point.y - last.y).toDouble()) >= 0.004) {
            points.add(point)
        }
        // Bound memory and decoding cost for long, shaky swipes.
        if (points.size > 180) {
            val reduced = points.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
            points.clear(); points.addAll(reduced)
        }
    }

    private fun keyCenterMap(): Map<Char, SwipeTypingEngine.Point> {
        val origin = IntArray(2)
        getLocationOnScreen(origin)
        return keyViews.mapNotNull { key ->
            val label = key.getLabel()
            if (label.length != 1 || !label[0].isLetter()) return@mapNotNull null
            val loc = IntArray(2); key.getLocationOnScreen(loc)
            label.lowercase()[0] to SwipeTypingEngine.Point(
                ((loc[0] + key.width / 2f - origin[0]) / width.coerceAtLeast(1)).coerceIn(0f, 1f),
                ((loc[1] + key.height / 2f - origin[1]) / height.coerceAtLeast(1)).coerceIn(0f, 1f)
            )
        }.toMap()
    }

    /** Snapshot key bounds once per gesture rather than rebuilding them for every motion event. */
    private fun captureLetterBounds(): List<Pair<android.graphics.Rect, Char>> = keyViews.mapNotNull { key ->
        val label = key.getLabel()
        if (label.length != 1 || !label[0].isLetter()) return@mapNotNull null
        val location = IntArray(2)
        key.getLocationOnScreen(location)
        android.graphics.Rect(location[0], location[1], location[0] + key.width, location[1] + key.height) to label[0].lowercaseChar()
    }

    private fun sampleSwipePath(
        x1: Float, y1: Float, x2: Float, y2: Float, path: MutableList<Char>,
        letterBounds: List<Pair<android.graphics.Rect, Char>>
    ) {
        val distance = kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        val samples = (distance / dp(6).coerceAtLeast(1)).toInt().coerceIn(1, 96)
        for (i in 1..samples) {
            val fraction = i.toFloat() / samples
            val x = (x1 + (x2 - x1) * fraction).toInt()
            val y = (y1 + (y2 - y1) * fraction).toInt()
            // Prefer exact hit-testing, then bridge the small physical gaps between keys by
            // assigning the sample to its nearest key centre. Fast swipes otherwise jump across
            // gaps and silently omit letters from the lexical candidate path.
            val touched = letterBounds.firstOrNull { it.first.contains(x, y) }?.second
                ?: letterBounds.minByOrNull { (rect, _) ->
                    kotlin.math.hypot(
                        (x - (rect.left + rect.right) / 2f).toDouble(),
                        (y - (rect.top + rect.bottom) / 2f).toFloat().toDouble()
                    )
                }?.let { (rect, letter) ->
                    val dx = x - (rect.left + rect.right) / 2f
                    val dy = y - (rect.top + rect.bottom) / 2f
                    val radius = maxOf(rect.width(), rect.height()) * 0.72f
                    letter.takeIf { kotlin.math.hypot(dx.toDouble(), dy.toDouble()) <= radius }
                }
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
        if (height > 0) regularKeyboardHeightPx = height
        emojiMode = true
        stopBackspaceRepeat()
        removeAllViews()
        keyViews.clear()

        // GridView virtualizes emoji cells instead of allocating thousands of TextViews at once.
        // Category chips navigate the complete vertically scrollable Unicode palette.
        val categoryNames = listOf("Recent", "Smileys", "People", "Animals", "Food", "Travel", "Activities", "Objects", "Symbols", "Flags", "All")
        val categoryIcons = listOf("◷", "☺", "☻", "🐻", "🍎", "🚗", "⚽", "💡", "♥", "🏳", "☷")
        fun loadEmojiCategory(index: Int) {
            emojiCategory = index
            emojiItems = if (index == 0) (if (recentEmoji.isNotEmpty()) recentEmoji else UnicodeEmojiData.all.take(48)) else UnicodeEmojiData.all.filter { emoji ->
                val cp = emoji.codePointAt(0)
                when (index) {
                    1 -> cp in 0x1F600..0x1F64F || cp in 0x1F910..0x1F92F || cp in 0x1F970..0x1F97A || cp in 0x1F9D0..0x1F9E6
                    2 -> cp in 0x1F440..0x1F487 || cp in 0x1F590..0x1F5FF || cp in 0x1F900..0x1F9CF
                    3 -> cp in 0x1F400..0x1F43F || cp in 0x1F980..0x1F9AE || cp in 0x1F990..0x1F9A2
                    4 -> cp in 0x1F32D..0x1F37F || cp in 0x1F950..0x1F96F
                    5 -> cp in 0x1F680..0x1F6FF || cp in 0x1F300..0x1F320
                    6 -> cp in 0x1F3A0..0x1F3FF || cp in 0x1F93C..0x1F945
                    7 -> cp in 0x1F4A0..0x1F4FF || cp in 0x1F3E0..0x1F3EF || cp in 0x1F9E0..0x1F9FF
                    8 -> cp in 0x2600..0x27BF || cp in 0x1FA70..0x1FAFF
                    9 -> cp in 0x1F1E6..0x1F1FF
                    10 -> true
                    else -> false
                }
            }
            emojiAdapter?.notifyDataSetChanged()
            emojiGrid?.smoothScrollToPosition(0)
        }
        val categoryBar = android.widget.HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val categoryRow = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        categoryNames.indices.forEach { index ->
            val chip = TextView(context).apply {
                text = categoryIcons[index]
                textSize = 20f
                gravity = Gravity.CENTER
                contentDescription = categoryNames[index]
                setTextColor(Color.WHITE)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener {
                    loadEmojiCategory(index)
                    for (i in 0 until categoryRow.childCount) {
                        val item = categoryRow.getChildAt(i) as TextView
                        item.background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = dp(18).toFloat()
                            setColor(if (i == index) Color.rgb(76, 86, 112) else Color.rgb(38, 39, 43))
                        }
                    }
                }
            }
            categoryRow.addView(chip, LayoutParams(LayoutParams.WRAP_CONTENT, dp(42)).apply {
                setMargins(dp(3), dp(2), dp(3), dp(2))
            })
        }
        categoryBar.addView(categoryRow)
        addView(categoryBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(34)))
        loadEmojiCategory(emojiCategory)
        for (i in 0 until categoryRow.childCount) {
            val item = categoryRow.getChildAt(i) as TextView
            item.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(if (i == emojiCategory) Color.rgb(76, 86, 112) else Color.rgb(38, 39, 43))
            }
        }
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
        emojiGrid = grid
        emojiAdapter = grid.adapter as? BaseAdapter
        // Initial fallback is replaced by resizeEmojiGridToKeyboard() after measurement.
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, dp(keyHeightDp * 2 + 12)))
        post { resizeEmojiGridToKeyboard() }

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
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, dp(keyHeightDp + 6)))
    }

    /** Keep the emoji palette the same total height as the normal keyboard as settings/window size change. */
    private fun resizeEmojiGridToKeyboard() {
        val grid = emojiGrid ?: return
        val measuredKeyboardHeight = regularKeyboardHeightPx.takeIf { it > 0 } ?: height
        if (measuredKeyboardHeight <= 0) return
        val categoryHeight = dp(34)
        val controlsHeight = dp(keyHeightDp + 6)
        val availableGridHeight = measuredKeyboardHeight - paddingTop - paddingBottom - categoryHeight - controlsHeight
        val targetHeight = availableGridHeight.coerceAtLeast(dp(keyHeightDp))
        val params = grid.layoutParams
        if (params.height != targetHeight) {
            params.height = targetHeight
            grid.layoutParams = params
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!emojiMode && height > 0) regularKeyboardHeightPx = height
        if (emojiMode) post { resizeEmojiGridToKeyboard() }
    }

    fun setRecentEmoji(values: List<String>) {
        recentEmoji = values.distinct().take(24)
        if (emojiMode) {
            if (emojiCategory == 0) {
                emojiItems = if (recentEmoji.isNotEmpty()) recentEmoji else UnicodeEmojiData.all.take(48)
                emojiAdapter?.notifyDataSetChanged()
            }
        }
    }

    fun setPreferences(
        haptics: Boolean,
        keyHeight: Int,
        color: String = keyColor,
        shape: String = keyShape,
        pressEffect: Boolean = pressEffectEnabled
    ) {
        hapticsEnabled = haptics
        keyHeightDp = keyHeight.coerceIn(40, 64)
        keyColor = color.takeIf { it in KeyboardPreferences.KEY_COLORS } ?: KeyboardPreferences.COLOR_GRAPHITE
        keyShape = shape.takeIf { it in KeyboardPreferences.KEY_SHAPES } ?: KeyboardPreferences.SHAPE_ROUNDED
        pressEffectEnabled = pressEffect
        keyViews.forEach {
            it.hapticsEnabled = hapticsEnabled
            it.keyHeightDp = keyHeightDp
            it.keyColor = keyColor
            it.keyShape = keyShape
            it.pressEffectEnabled = pressEffectEnabled
        }
        if (!emojiMode && height > 0) regularKeyboardHeightPx = height
        if (emojiMode) post { resizeEmojiGridToKeyboard() }
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
        const val CURSOR_STEP_DP = 10
        const val DELETE_PREVIEW_START_DP = 8f
        const val DELETE_PREVIEW_CHAR_DP = 10f
        const val BACKSPACE_REPEAT_START_DELAY_MS = 340L
        const val BACKSPACE_REPEAT_INTERVAL_MS = 42L
        const val BACKSPACE_REPEAT_FAST_INTERVAL_MS = 28L
        const val BACKSPACE_ACCELERATION_COUNT = 5
    }
}
