package com.typeflow.keyboard

/** Pure gesture math shared by touch handling and local regression tests. */
internal object GestureMath {
    fun cursorSteps(deltaX: Float, keyWidthPx: Float): Int {
        if (!deltaX.isFinite() || !keyWidthPx.isFinite() || keyWidthPx <= 0f) return 0
        return (deltaX / keyWidthPx).toInt()
    }
}
