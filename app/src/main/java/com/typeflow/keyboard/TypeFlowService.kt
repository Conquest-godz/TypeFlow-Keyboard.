package com.typeflow.keyboard

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

class TypeFlowService : InputMethodService() {
    private var shiftEnabled = false
    private var capsLock = false
    private var numberMode = false
    private var lastShiftTap = 0L
    private var keyboard: KeyboardView? = null

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        keyboard = view

        view.setKeyListener { key ->
            // Layout navigation must work even if an editor temporarily has no connection.
            when (key) {
                "😀" -> {
                    view.showEmoji()
                    return@setKeyListener
                }
                "ABC" -> {
                    numberMode = false
                    view.showLetters()
                    return@setKeyListener
                }
                "?123" -> {
                    numberMode = true
                    view.showNumbers()
                    return@setKeyListener
                }
            }

            val ic = currentInputConnection ?: return@setKeyListener
            when {
                key.startsWith("E:") -> {
                    ic.commitText(key.removePrefix("E:"), 1)
                    view.showLetters()
                }
                key == "SPACE" -> {
                    ic.commitText(" ", 1)
                    consumeOneShotShift()
                }
                key == "⌫" -> {
                    val selected = ic.getSelectedText(0)
                    if (!selected.isNullOrEmpty()) {
                        ic.commitText("", 1)
                    } else {
                        ic.deleteSurroundingTextInCodePoints(1, 0)
                    }
                }
                key == "↵" -> handleEnter(ic)
                key == "⇧" -> handleShift()
                else -> {
                    val output = if (!numberMode && key.length == 1 && key[0].isLetter()) {
                        if (capsLock || shiftEnabled) key.uppercase() else key.lowercase()
                    } else key
                    ic.commitText(output, 1)
                    if (!numberMode && key.length == 1 && key[0].isLetter()) consumeOneShotShift()
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
            numberMode = false
            keyboard?.showLetters()
        }
    }

    override fun onFinishInput() {
        keyboard = null
        shiftEnabled = false
        capsLock = false
        numberMode = false
        super.onFinishInput()
    }

    private fun consumeOneShotShift() {
        if (shiftEnabled && !capsLock && !numberMode) shiftEnabled = false
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
    }

    private fun shouldAutoCapitalize(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val capSentenceFlag = inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        return inputClass == InputType.TYPE_CLASS_TEXT && capSentenceFlag != 0
    }

    private fun handleEnter(ic: InputConnection) {
        val options = currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
        val action = options and EditorInfo.IME_MASK_ACTION
        val noEnterAction = options and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        val inputType = currentInputEditorInfo?.inputType ?: InputType.TYPE_CLASS_TEXT
        val multiline = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
            inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0

        if (noEnterAction || multiline) {
            ic.commitText("\n", 1)
            return
        }

        when (action) {
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_PREVIOUS -> {
                if (!ic.performEditorAction(action)) ic.commitText("\n", 1)
            }
            else -> ic.commitText("\n", 1)
        }
    }
}
