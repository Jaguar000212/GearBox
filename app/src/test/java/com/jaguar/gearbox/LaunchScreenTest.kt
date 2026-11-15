package com.jaguar.gearbox

import com.jaguar.gearbox.data.LaunchScreen
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchScreenTest {
    @Test fun absentAndUnknownPreferenceUseHome() {
        listOf(null, "", "unknown", "settings", "favorites").forEach {
            assertEquals(LaunchScreen.HOME, LaunchScreen.fromStored(it))
        }
    }
    @Test fun bothOptionsRoundTripWithStableRoutes() {
        LaunchScreen.entries.forEach { assertEquals(it, LaunchScreen.fromStored(it.name)) }
        assertEquals("home", LaunchScreen.HOME.route)
        assertEquals("favorites", LaunchScreen.FAVORITES.route)
    }
}
