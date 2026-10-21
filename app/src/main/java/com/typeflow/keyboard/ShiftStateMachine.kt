package com.typeflow.keyboard

/**
 * Deterministic Shift/Caps Lock state for an IME.
 *
 * The caller supplies monotonic timestamps so the state machine is testable
 * without Android framework dependencies.
 */
internal class ShiftStateMachine(
    private val doubleTapTimeoutMs: Long = DEFAULT_DOUBLE_TAP_TIMEOUT_MS
) {
    enum class State { OFF, SHIFT, CAPS_LOCK }

    var state: State = State.OFF
        private set

    private var lastShiftTapMs: Long? = null

    fun onInputStarted(autoCapitalize: Boolean) {
        state = if (autoCapitalize) State.SHIFT else State.OFF
        lastShiftTapMs = null
    }

    fun onShiftTap(nowMs: Long) {
        val previousTap = lastShiftTapMs
        val isDoubleTap = state == State.SHIFT &&
            previousTap != null &&
            nowMs >= previousTap &&
            nowMs - previousTap <= doubleTapTimeoutMs

        state = when {
            isDoubleTap -> State.CAPS_LOCK
            state == State.CAPS_LOCK -> State.OFF
            state == State.SHIFT -> State.OFF
            else -> State.SHIFT
        }
        lastShiftTapMs = if (isDoubleTap || state == State.OFF || state == State.CAPS_LOCK) {
            null
        } else {
            nowMs
        }
    }

    fun onCharacterCommitted() {
        if (state == State.SHIFT) state = State.OFF
        // Typing must end the double-tap window, even when Caps Lock remains on.
        lastShiftTapMs = null
    }

    companion object {
        const val DEFAULT_DOUBLE_TAP_TIMEOUT_MS = 350L
    }
}
