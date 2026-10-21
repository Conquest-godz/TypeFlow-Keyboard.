package com.typeflow.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * Resolves whether Enter should insert a newline or invoke an editor action.
 * A null action means the keyboard should insert a newline.
 */
internal object EditorActionResolver {
    data class Resolution(
        val action: Int?,
        val label: String
    ) {
        val insertsNewline: Boolean get() = action == null
    }

    fun resolve(info: EditorInfo?): Resolution {
        if (info == null) return Resolution(null, "Enter")

        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        val isMultilineText = inputClass == InputType.TYPE_CLASS_TEXT &&
            (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val noEnterAction = info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (isMultilineText || noEnterAction) return Resolution(null, "Enter")

        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_GO -> Resolution(action, "Go")
            EditorInfo.IME_ACTION_SEARCH -> Resolution(action, "Search")
            EditorInfo.IME_ACTION_SEND -> Resolution(action, "Send")
            EditorInfo.IME_ACTION_NEXT -> Resolution(action, "Next")
            EditorInfo.IME_ACTION_DONE -> Resolution(action, "Done")
            EditorInfo.IME_ACTION_PREVIOUS -> Resolution(action, "Previous")
            else -> Resolution(null, "Enter")
        }
    }
}
