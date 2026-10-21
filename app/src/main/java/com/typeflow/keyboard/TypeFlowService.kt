package com.typeflow.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.os.SystemClock

class TypeFlowService : InputMethodService() {
    private var shiftEnabled = false
    private var capsLock = false
    private var numberMode = false
    private var lastShiftTap = 0L
    private var keyboard: KeyboardView? = null

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        keyboard = view
        updateShiftAppearance()

        view.setKeyListener { key ->
            val ic = currentInputConnection ?: return@setKeyListener
            when (key) {
                "SPACE" -> ic.commitText(" ", 1)
                "⌫" -> ic.deleteSurroundingText(1, 0)
                "↵" -> handleEnter(ic)
                "⇧" -> handleShift()
                "?123" -> {
                    numberMode = true
                    view.showNumbers()
                }
                "ABC" -> {
                    numberMode = false
                    view.showLetters()
                    updateShiftAppearance()
                }
                "😀" -> {
                    // Emoji picker is not implemented in this baseline.
                }
                else -> {
                    val output = if (!numberMode && key.length == 1 && key[0].isLetter()) {
                        if (capsLock || shiftEnabled) key.uppercase() else key.lowercase()
                    } else {
                        key
                    }
                    ic.commitText(output, 1)
                    if (shiftEnabled && !capsLock && !numberMode) {
                        shiftEnabled = false
                        updateShiftAppearance()
                    }
                }
            }
        }
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting) {
            shiftEnabled = shouldAutoCapitalize(attribute)
            capsLock = false
            lastShiftTap = 0L
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        shiftEnabled = false
        capsLock = false
        keyboard = null
    }

    private fun handleShift() {
        val now = SystemClock.uptimeMillis()
        if (now - lastShiftTap in 1..350) {
            capsLock = !capsLock
            shiftEnabled = capsLock
            lastShiftTap = 0L
        } else {
            shiftEnabled = !shiftEnabled
            lastShiftTap = now
        }
        updateShiftAppearance()
    }

    private fun updateShiftAppearance() {
        // State is applied when the key is next rendered; a dedicated active key style is planned.
    }

    private fun shouldAutoCapitalize(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val variation = inputType and InputType.TYPE_MASK_CLASS
        val textFlags = inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        return variation == InputType.TYPE_CLASS_TEXT && textFlags != 0
    }

    private fun handleEnter(ic: InputConnection) {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        when (action) {
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_PREVIOUS -> {
                if (!ic.performEditorAction(action)) {
                    ic.commitText("\n", 1)
                }
            }
            else -> ic.commitText("\n", 1)
        }
    }
}
