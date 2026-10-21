package com.typeflow.keyboard

import org.junit.Assert.assertTrue
import org.junit.Test

class UnicodeEmojiDataTest {
    @Test fun includesThousandsOfUnicodeEmoji() {
        assertTrue("Expected a broad Unicode emoji set", UnicodeEmojiData.all.size >= 3700)
    }

    @Test fun includesMultiCodePointFamilyAndFlagSequences() {
        assertTrue(UnicodeEmojiData.all.contains("👨‍👩‍👧‍👦"))
        assertTrue(UnicodeEmojiData.all.contains("🏴‍☠️"))
    }
}
