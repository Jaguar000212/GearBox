package com.jaguar.gearbox.tiles

import org.junit.Assert.*
import org.junit.Test

class ClockTileLaunchTest {
    @Test fun onlyTwoLaunchOnlyRoutesAreRecognized() {
        assertEquals(ClockTileLaunch.TIMER, ClockTileLaunch.fromRoute("quick_tile_timer"))
        assertEquals(ClockTileLaunch.STOPWATCH, ClockTileLaunch.fromRoute("quick_tile_stopwatch"))
        listOf(null, "timer_countdown_notification", "stopwatch_notification_reset", "tool/timer_stopwatch", "RESET")
            .forEach { assertNull(ClockTileLaunch.fromRoute(it)) }
        assertNotEquals(ClockTileLaunch.TIMER.requestCode, ClockTileLaunch.STOPWATCH.requestCode)
    }
}
