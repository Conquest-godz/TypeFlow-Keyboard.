package com.typeflow.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnicodeEmojiDataTest {
    @Test fun paletteContainsThousandsOfUniqueUnicodeEmoji() {
        assertTrue("Emoji palette should cover the Unicode emoji set", UnicodeEmojiData.all.size >= 3700)
        assertEquals("Emoji entries should not be duplicated", UnicodeEmojiData.all.size, UnicodeEmojiData.all.toSet().size)
    }

    @Test fun paletteIncludesSkinToneVariantsAndJoinedSequences() {
        assertTrue(UnicodeEmojiData.all.any { it.contains("🏻") || it.contains("🏿") })
        assertTrue(UnicodeEmojiData.all.any { it.contains("‍") })
        assertTrue(UnicodeEmojiData.all.contains("👨‍👩‍👧‍👦"))
    }

    @Test fun paletteIncludesRegionalFlagsAndNewerEmoji() {
        assertTrue(UnicodeEmojiData.all.contains("🇺🇸"))
        assertTrue(UnicodeEmojiData.all.contains("🫨"))
        assertTrue(UnicodeEmojiData.all.contains("🏴‍☠️"))
    }
}
