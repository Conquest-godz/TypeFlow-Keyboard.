package com.typeflow.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeTypingEngineTest {
    @Test fun collapsesRepeatedAdjacentKeyVisits() {
        assertEquals("helo", SwipeTypingEngine.normalizePath(listOf('h', 'e', 'e', 'l', 'l', 'o')))
    }

    @Test fun recognizesKnownSwipePathOffline() {
        assertEquals("hello", SwipeTypingEngine.recognize(listOf('h', 'e', 'l', 'l', 'o')))
    }

    @Test fun ignoresTooShortPaths() {
        assertNull(SwipeTypingEngine.recognize(listOf('a')))
    }
}
