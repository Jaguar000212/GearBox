package com.jaguar.gearbox.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultUnitSystemTest {
    @Test fun legacyAndInvalidValuesKeepMetricBehavior() {
        listOf(null, "", "metric", "unknown").forEach {
            assertEquals(DefaultUnitSystem.METRIC, DefaultUnitSystem.fromStored(it))
        }
        assertEquals(DefaultUnitSystem.METRIC, UiPreferences().defaultUnitSystem)
    }
    @Test fun onlyKnownStoredValuesAreAccepted() {
        assertEquals(DefaultUnitSystem.IMPERIAL, DefaultUnitSystem.fromStored("IMPERIAL"))
        assertEquals(DefaultUnitSystem.METRIC, DefaultUnitSystem.fromStored("METRIC"))
    }
}
