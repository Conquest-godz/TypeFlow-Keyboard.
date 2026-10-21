package com.typeflow.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class SwipeTypingEngineTest {
    @Test fun collapsesRepeatedAdjacentKeyVisits() {
        assertEquals("helo", SwipeTypingEngine.normalizePath(listOf('h', 'e', 'e', 'l', 'l', 'o')))
    }

    @Test fun recognizesKnownSwipePathOffline() {
        assertEquals("hello", SwipeTypingEngine.recognize(listOf('h', 'e', 'l', 'l', 'o')))
    }

    @Test fun loadsOfflineVocabularyAndRecognizesWordsOutsideSeedSet() {
        SwipeTypingEngine.loadDictionary(ByteArrayInputStream("hello\nworld\n".toByteArray()))
        assertEquals("world", SwipeTypingEngine.recognize(listOf('w', 'o', 'r', 'l', 'd')))
    }

    @Test fun recognizesACommonGlideThatSkipsAnIntermediateKey() {
        assertEquals("hello", SwipeTypingEngine.recognize(listOf('h', 'e', 'l', 'o')))
    }

    @Test fun fallbackHandlesFastSwipeThatSkipsSeveralSamples() {
        // A key-only trace h-e-o has no geometric evidence to distinguish hero from hello.
        assertTrue(SwipeTypingEngine.recognize(listOf('h', 'e', 'o')) in setOf("hero", "hello"))
    }


    @Test fun ignoresTooShortPaths() {
        assertNull(SwipeTypingEngine.recognize(listOf('a')))
    }

    private fun qwertyCenters(): Map<Char, SwipeTypingEngine.Point> {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val result = mutableMapOf<Char, SwipeTypingEngine.Point>()
        rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, char ->
                val rowOffset = when (rowIndex) {
                    1 -> 0.045f
                    2 -> 0.10f
                    else -> 0f
                }
                result[char] = SwipeTypingEngine.Point(
                    (columnIndex + 0.5f) / 10f + rowOffset,
                    0.25f + rowIndex * 0.25f
                )
            }
        }
        return result
    }

    @Test fun spatialDecoderRecognizesExactKnownTrace() {
        val centers = qwertyCenters()
        val trace = "hello".map { centers.getValue(it) }
        assertEquals("hello", SwipeTypingEngine.recognizeSpatial("hello".toList(), trace, centers))
    }

    @Test fun spatialDecoderRecognizesTraceWhenVisitedKeysSkipRepeatedLetter() {
        val centers = qwertyCenters()
        val trace = "hello".map { centers.getValue(it) }
        assertEquals("hello", SwipeTypingEngine.recognizeSpatial("helo".toList(), trace, centers))
    }

    @Test fun spatialDecoderRetriesWhenNearestEndpointIsWrong() {
        val centers = qwertyCenters()
        val trace = "hello".map { centers.getValue(it) }.toMutableList()
        // Slightly bias the first sample toward adjacent 'g' while keeping it close to 'h'.
        // The trace should still be recoverable by the tolerant endpoint pass.
        val h = centers.getValue('h')
        val g = centers.getValue('g')
        trace[0] = SwipeTypingEngine.Point(h.x * 0.55f + g.x * 0.45f, h.y)
        assertEquals("hello", SwipeTypingEngine.recognizeSpatial("hello".toList(), trace, centers))
    }

    @Test fun spatialDecoderRejectsTooShortTrace() {
        val centers = qwertyCenters()
        assertNull(
            SwipeTypingEngine.recognizeSpatial(
                listOf('h', 'o'),
                listOf(centers.getValue('h'), centers.getValue('o')),
                centers
            )
        )
    }

    @Test fun spatialDecoderRejectsInsufficientKeyCenters() {
        val centers = qwertyCenters().filterKeys { it in "hello" }
        assertNull(
            SwipeTypingEngine.recognizeSpatial(
                "hello".toList(),
                "hello".map { centers.getValue(it) },
                centers
            )
        )
    }
}