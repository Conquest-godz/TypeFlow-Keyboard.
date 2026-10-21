package com.typeflow.keyboard

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class TypeFlowService : InputMethodService() {
    private val shiftState = ShiftStateMachine(ViewConfiguration.getDoubleTapTimeout().toLong())
    private var numberMode = false
    private var keyboard: KeyboardView? = null
    private lateinit var preferences: KeyboardPreferences
    private lateinit var clipboardHistory: ClipboardHistoryStore

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        clipboardHistory = ClipboardHistoryStore(this)
        runCatching { SwipeTypingEngine.loadDictionary(assets.open("words.txt")) }
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        keyboard = view
        view.setPreferences(preferences.hapticsEnabled, preferences.keyHeightDp)
        view.setRecentEmoji(preferences.recentEmoji())
        view.setKeyListener { key ->
            when (key) {
                "😀" -> { view.showEmoji(); return@setKeyListener }
                "ABC" -> { numberMode = false; view.showLetters(); syncShiftState(); return@setKeyListener }
                "?123" -> { numberMode = true; view.showNumbers(); return@setKeyListener }
            }
            if (key.startsWith("CAT:")) return@setKeyListener
            val ic = currentInputConnection ?: return@setKeyListener
            when {
                key.startsWith("CURSOR:") -> moveCursor(ic, key.removePrefix("CURSOR:").toIntOrNull() ?: 0)
                key.startsWith("ALT:") -> {
                    val alternate = key.removePrefix("ALT:")
                    if (alternate != "CANCEL") ic.commitText(alternate, 1)
                    shiftState.onCharacterCommitted(); syncShiftState()
                }
                key.startsWith("E:") -> {
                    val emoji = key.removePrefix("E:")
                    ic.commitText(emoji, 1)
                    preferences.rememberEmoji(emoji)
                    view.setRecentEmoji(preferences.recentEmoji())
                    syncShiftState()
                }
                key.startsWith("SWIPEWORD:") -> {
                    val word = key.removePrefix("SWIPEWORD:")
                    val output = when (shiftState.state) {
                        ShiftStateMachine.State.OFF -> word
                        ShiftStateMachine.State.SHIFT -> word.replaceFirstChar { it.uppercase() }
                        ShiftStateMachine.State.CAPS_LOCK -> word.uppercase()
                    }
                    ic.commitText(output, 1)
                    shiftState.onCharacterCommitted(); syncShiftState()
                }
                key == "SPACE" -> { ic.commitText(" ", 1); shiftState.onCharacterCommitted(); syncShiftState() }
                key == "⌫" -> deleteBackward(ic)
                key == "DELETE_WORD" -> deletePreviousWord(ic)
                key == "↵" || key in listOf("Go", "Search", "Send", "Next", "Done", "Previous") -> handleEnter(ic)
                key == "⇧" -> { shiftState.onShiftTap(SystemClock.uptimeMillis()); syncShiftState() }
                else -> {
                    val output = if (!numberMode && key.length == 1 && key[0].isLetter()) {
                        if (shiftState.state != ShiftStateMachine.State.OFF) key.uppercase() else key.lowercase()
                    } else key
                    ic.commitText(output, 1)
                    if (!numberMode && key.length == 1) { shiftState.onCharacterCommitted(); syncShiftState() }
                }
            }
        }
        syncShiftState()
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (::preferences.isInitialized) {
            keyboard?.setPreferences(preferences.hapticsEnabled, preferences.keyHeightDp)
            keyboard?.setRecentEmoji(preferences.recentEmoji())
        }
        if (!restarting) {
            shiftState.onInputStarted(shouldAutoCapitalize(attribute))
            numberMode = false
            keyboard?.showLetters()
        }
        syncShiftState()
    }

    override fun onFinishInput() {
        keyboard?.cancelPendingGestures()
        super.onFinishInput()
        shiftState.onInputStarted(autoCapitalize = false)
        numberMode = false
    }

    override fun onDestroy() {
        if (::clipboardHistory.isInitialized) clipboardHistory.close()
        super.onDestroy()
    }

    private fun syncShiftState() {
        keyboard?.setShiftState(shiftState.state)
        keyboard?.updateEnterKey(EditorActionResolver.resolve(currentInputEditorInfo).label)
    }

    private fun shouldAutoCapitalize(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        return (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
            (inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
    }

    private fun moveCursor(ic: InputConnection, steps: Int) {
        val safeSteps = steps.coerceIn(-20, 20)
        val keyCode = if (safeSteps < 0) android.view.KeyEvent.KEYCODE_DPAD_LEFT else android.view.KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(safeSteps)) {
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
        }
    }

    private fun deleteBackward(ic: InputConnection) {
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) ic.commitText("", 1)
        else ic.deleteSurroundingTextInCodePoints(1, 0)
    }

    private fun deletePreviousWord(ic: InputConnection) {
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(256, 0)?.toString().orEmpty()
        if (before.isEmpty()) return
        var end = before.length
        while (end > 0 && before[end - 1].isWhitespace()) end--
        while (end > 0 && !before[end - 1].isWhitespace()) end--
        while (end > 0 && before[end - 1].isWhitespace()) end--
        val count = before.length - end
        if (count > 0) ic.deleteSurroundingText(count, 0)
    }

    private fun handleEnter(ic: InputConnection) {
        val resolution = EditorActionResolver.resolve(currentInputEditorInfo)
        val action = resolution.action
        if (action == null || !ic.performEditorAction(action)) ic.commitText("\n", 1)
    }
}
