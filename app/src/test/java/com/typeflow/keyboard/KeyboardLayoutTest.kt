package com.typeflow.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun letterLayoutContainsExpectedControlKeys() {
        val keys = KeyboardLayout.letters.flatten()
        assertTrue(keys.contains("⇧"))
        assertTrue(keys.contains("⌫"))
        assertTrue(keys.contains("😀"))
        assertTrue(keys.contains("SPACE"))
        assertTrue(keys.contains("↵"))
    }

    @Test
    fun emojiLayoutContainsOnlySelectableEmojiOrNavigationControls() {
        val keys = KeyboardLayout.emoji.flatten()
        assertTrue(keys.isNotEmpty())
        assertTrue(keys.any { it == "ABC" })
        assertTrue(keys.any { it.startsWith("E:") })
        assertFalse(keys.any { it == "SPACE" })
    }

    @Test
    fun everyEmojiTokenHasContent() {
        KeyboardLayout.emoji.flatten()
            .filter { it.startsWith("E:") }
            .forEach { assertFalse(it.removePrefix("E:").isBlank()) }
    }
}
