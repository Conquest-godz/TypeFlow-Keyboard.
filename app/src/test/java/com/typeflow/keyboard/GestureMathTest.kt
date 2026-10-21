package com.typeflow.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureMathTest {
    @Test fun cursorStepsFollowHorizontalTravel() {
        assertEquals(3, GestureMath.cursorSteps(30f, 10f))
        assertEquals(-2, GestureMath.cursorSteps(-20f, 10f))
    }

    @Test fun tinyTravelAndInvalidWidthsDoNotMoveCursor() {
        assertEquals(0, GestureMath.cursorSteps(3f, 10f))
        assertEquals(0, GestureMath.cursorSteps(100f, 0f))
        assertEquals(0, GestureMath.cursorSteps(100f, -1f))
    }
}
