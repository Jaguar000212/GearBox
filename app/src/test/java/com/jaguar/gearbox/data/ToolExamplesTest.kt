package com.jaguar.gearbox.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ToolExamplesTest {
    @Test fun examplesAreBoundedInputOnlyAndHaveHelp() {
        assertEquals(24, ToolExampleCatalog.routes.size)
        ToolExampleCatalog.routes.forEach { route ->
            val example = ToolExampleCatalog.forRoute(route)!!
            assertTrue(ToolMemoryPolicy.permitsRoute(route))
            assertTrue(ToolMemoryPolicy.validInputs(example.inputs))
            assertTrue(example.description.isNotBlank())
            assertNotNull(ToolHelpCatalog.forRoute(route))
            assertFalse(example.inputs.keys.any { it in setOf("result", "output", "running", "score", "wins") })
        }
    }

    @Test fun sideEffectToolsHaveNoLoadableExample() {
        listOf(Tools.ROUTE_PASSWORD_GENERATOR, Tools.ROUTE_TIMER_STOPWATCH, Tools.ROUTE_DICE,
            Tools.ROUTE_RANDOM, Tools.ROUTE_SCOREBOARD, Tools.ROUTE_TAMBOLA, Tools.ROUTE_TEAM_PICKER,
            Tools.ROUTE_TIC_TAC_TOE, Tools.ROUTE_COMPASS, Tools.ROUTE_FLASHLIGHT, "unknown", null)
            .forEach { assertNull(ToolExampleCatalog.forRoute(it)) }
    }

    @Test fun dateExampleIsRelativeAndQrHashContainNoGeneratedResult() {
        val date = ToolExampleCatalog.forRoute(Tools.ROUTE_DAYS_UNTIL, LocalDate.of(2026, 12, 28))!!
        assertEquals("2027-01-04", date.inputs["targetDate"])
        assertEquals(setOf("text"), ToolExampleCatalog.forRoute(Tools.ROUTE_QR_GENERATOR)!!.inputs.keys)
        assertEquals(mapOf("text" to "Hello GearBox", "algorithm" to "SHA256"), ToolExampleCatalog.forRoute(Tools.ROUTE_HASH_GENERATOR)!!.inputs)
    }

    @Test fun unitExampleUsesActualRestorableUnitLabels() {
        assertEquals(mapOf("category" to "LENGTH", "fromUnit" to "Kilometers", "toUnit" to "Meters", "input" to "1"),
            ToolExampleCatalog.forRoute(Tools.ROUTE_UNIT_CONVERTER)!!.inputs)
    }
    @Test fun colorBlendExampleContainsOnlyValidatedInputs() {
        val example = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_COLOR_BLEND))
        assertEquals(mapOf("firstHex" to "#000000", "secondHex" to "#FFFFFF", "weight" to "50"), example.inputs)
        assertNotNull(parseColorBlendSnapshot(example.inputs))
    }
}
