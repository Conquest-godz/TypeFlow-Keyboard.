package com.typeflow.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureMathTest {
    @Test
    fun cursorStepsFollowHorizontalTravel() {
        assertEquals(3, GestureMath.cursorSteps(deltaX = 30f, keyWidthPx = 10f))
        assertEquals(-2, GestureMath.cursorSteps(deltaX = -20f, keyWidthPx = 10f))
    }

    @Test
    fun cursorMovementIgnoresTinyTravel() {
        assertEquals(0, GestureMath.cursorSteps(deltaX = 3f, keyWidthPx = 10f))
    }

    @Test
    fun invalidKeyWidthNeverProducesMovement() {
        assertEquals(0, GestureMath.cursorSteps(deltaX = 100f, keyWidthPx = 0f))
        assertEquals(0, GestureMath.cursorSteps(deltaX = 100f, keyWidthPx = -1f))
    }
}
