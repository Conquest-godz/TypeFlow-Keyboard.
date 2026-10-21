package com.typeflow.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftStateMachineTest {
    @Test fun singleTapTogglesShiftAndTypingConsumesIt() {
        val machine = ShiftStateMachine()
        machine.onShiftTap(100)
        assertEquals(ShiftStateMachine.State.SHIFT, machine.state)
        machine.onCharacterCommitted()
        assertEquals(ShiftStateMachine.State.OFF, machine.state)
        machine.onShiftTap(200)
        assertEquals(ShiftStateMachine.State.SHIFT, machine.state)
        machine.onCharacterCommitted()
        assertEquals(ShiftStateMachine.State.OFF, machine.state)
    }

    @Test fun doubleTapEnablesCapsLockAndNextTapDisablesIt() {
        val machine = ShiftStateMachine()
        machine.onShiftTap(100)
        machine.onShiftTap(300)
        assertEquals(ShiftStateMachine.State.CAPS_LOCK, machine.state)
        machine.onShiftTap(350)
        assertEquals(ShiftStateMachine.State.OFF, machine.state)
    }

    @Test fun typingInvalidatesDoubleTapWindow() {
        val machine = ShiftStateMachine()
        machine.onShiftTap(100)
        machine.onCharacterCommitted()
        machine.onShiftTap(200)
        assertEquals(ShiftStateMachine.State.SHIFT, machine.state)
    }

    @Test fun tapAfterTimeoutDoesNotEnableCapsLock() {
        val machine = ShiftStateMachine()
        machine.onShiftTap(100)
        machine.onShiftTap(451)
        assertEquals(ShiftStateMachine.State.OFF, machine.state)
    }

    @Test fun newInputResetsStateToAutoCapitalizationPreference() {
        val machine = ShiftStateMachine()
        machine.onShiftTap(100)
        machine.onShiftTap(200)
        machine.onInputStarted(autoCapitalize = true)
        assertEquals(ShiftStateMachine.State.SHIFT, machine.state)
        machine.onInputStarted(autoCapitalize = false)
        assertEquals(ShiftStateMachine.State.OFF, machine.state)
    }
}
