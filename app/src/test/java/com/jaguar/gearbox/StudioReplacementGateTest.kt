package com.jaguar.gearbox

import com.jaguar.gearbox.images.StudioReplacementGate
import org.junit.Assert.*
import org.junit.Test

class StudioReplacementGateTest {
    @Test fun requestIdentityIsSeparateFromEditorGeneration() {
        val gate = StudioReplacementGate()
        val first = gate.begin(7, false)
        assertTrue(gate.current(first, 7, false))
        assertFalse(gate.current(first, 8, false))
        assertFalse(gate.current(first, 7, true))
        gate.cancel()
        val next = gate.begin(7, false)
        assertFalse(gate.current(first, 7, false))
        assertFalse(gate.finish(first))
        assertTrue(gate.current(next, 7, false))
        assertTrue(gate.finish(next))
        assertFalse(gate.current(next, 7, false))
    }
}
