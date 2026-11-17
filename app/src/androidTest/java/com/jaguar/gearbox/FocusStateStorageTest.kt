package com.jaguar.gearbox

import com.jaguar.gearbox.focus.*
import org.junit.Assert.*
import org.junit.Test

/** Android JSONObject parsing; no alarms, notifications or production storage are touched. */
class FocusStateStorageTest {
    @Test fun validCurrentSessionRoundTrips() {
        val start = startFocus(FocusConfig(rounds = 5), 1234, 5, 10)
        assertEquals(start, decodeFocusState(encodeFocusState(start)))
        assertEquals(FocusState(), decodeFocusState(encodeFocusState(FocusState())))
        val paused = pauseFocus(start, 2234, 5)
        assertEquals(paused, decodeFocusState(encodeFocusState(paused)))
    }
    @Test fun malformedFutureAndCoercedStateFailsClosed() {
        val start = startFocus(FocusConfig(), 1000, 1, 0)
        assertNull(decodeFocusState(encodeFocusState(start).put("phase", 999)))
        assertNull(decodeFocusState(encodeFocusState(start).put("remaining", "1000")))
        assertNull(decodeFocusState(encodeFocusState(start).put("remaining", 1.5)))
        assertNull(decodeFocusState(encodeFocusState(start).put("alerted", 10)))
        assertNull(decodeFocusState(encodeFocusState(start).put("version", 2)))
        assertNull(decodeFocusState(encodeFocusState(start).put("unknown", "unsupported")))
        assertNull(decodeFocusState(encodeFocusState(start).put("rounds", 100)))
    }
}
