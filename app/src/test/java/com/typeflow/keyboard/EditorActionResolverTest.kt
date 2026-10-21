package com.typeflow.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorActionResolverTest {
    @Test fun nullEditorUsesNewline() {
        assertTrue(EditorActionResolver.resolve(null).insertsNewline)
    }

    @Test fun mapsSearchAndSendActions() {
        val search = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH }
        val send = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEND }
        assertEquals("Search", EditorActionResolver.resolve(search).label)
        assertEquals(EditorInfo.IME_ACTION_SEARCH, EditorActionResolver.resolve(search).action)
        assertEquals("Send", EditorActionResolver.resolve(send).label)
    }

    @Test fun multilineAlwaysUsesNewline() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        assertTrue(EditorActionResolver.resolve(info).insertsNewline)
    }

    @Test fun noEnterActionFlagPreservesNewline() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        }
        assertTrue(EditorActionResolver.resolve(info).insertsNewline)
    }

    @Test fun unspecifiedAndNoneUseNewline() {
        val unspecified = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED }
        val none = EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_NONE }
        assertTrue(EditorActionResolver.resolve(unspecified).insertsNewline)
        assertTrue(EditorActionResolver.resolve(none).insertsNewline)
    }
}
