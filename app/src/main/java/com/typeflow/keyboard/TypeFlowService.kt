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

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        keyboard = view
        view.setKeyListener { key ->
            val ic = currentInputConnection ?: return@setKeyListener
            when (key) {
                "SPACE" -> {
                    ic.commitText(" ", 1)
                    shiftState.onCharacterCommitted()
                    syncShiftState()
                }
                "⌫" -> deleteBackward(ic)
                "↵" -> handleEnter(ic)
                "⇧" -> {
                    shiftState.onShiftTap(SystemClock.uptimeMillis())
                    syncShiftState()
                }
                "?123" -> {
                    numberMode = true
                    view.showNumbers()
                }
                "ABC" -> {
                    numberMode = false
                    view.showLetters()
                    syncShiftState()
                }
                "😀" -> {
                    // Emoji picker is not implemented in this baseline.
                }
                else -> {
                    val output = if (!numberMode && key.length == 1 && key[0].isLetter()) {
                        if (shiftState.state != ShiftStateMachine.State.OFF) key.uppercase()
                        else key.lowercase()
                    } else {
                        key
                    }
                    ic.commitText(output, 1)
                    if (!numberMode && key.length == 1 && (key[0].isLetter() || !key[0].isLetterOrDigit())) {
                        // A one-shot Shift applies to punctuation too, not only letters.
                        shiftState.onCharacterCommitted()
                        syncShiftState()
                    }
                }
            }
        }
        syncShiftState()
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting) {
            shiftState.onInputStarted(shouldAutoCapitalize(attribute))
            numberMode = false
            syncShiftState()
        }
    }

    override fun onFinishInput() {
        keyboard?.stopBackspaceRepeat()
        super.onFinishInput()
        shiftState.onInputStarted(autoCapitalize = false)
        numberMode = false
        // Keep the view reference: Android may reuse the same input view for the next field.
    }

    private fun syncShiftState() {
        keyboard?.setShiftState(shiftState.state)
        keyboard?.updateEnterKey(EditorActionResolver.resolve(currentInputEditorInfo).label)
    }

    private fun shouldAutoCapitalize(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val capSentenceFlag = inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        return inputClass == InputType.TYPE_CLASS_TEXT && capSentenceFlag != 0
    }

    private fun deleteBackward(ic: InputConnection) {
        // Clear a selection first; otherwise delete the preceding Unicode code point.
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingTextInCodePoints(1, 0)
        }
    }

    private fun handleEnter(ic: InputConnection) {
        val resolution = EditorActionResolver.resolve(currentInputEditorInfo)
        val action = resolution.action
        if (action == null || !ic.performEditorAction(action)) {
            ic.commitText("\n", 1)
        }
    }
}
