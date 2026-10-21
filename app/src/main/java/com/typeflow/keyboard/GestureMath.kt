package com.typeflow.keyboard

/** Pure cursor gesture math shared by touch handling and regression tests. */
internal object GestureMath {
    fun cursorSteps(deltaX: Float, keyWidthPx: Float): Int {
        if (!deltaX.isFinite() || !keyWidthPx.isFinite() || keyWidthPx <= 0f) return 0
        return (deltaX / keyWidthPx).toInt()
    }
}
