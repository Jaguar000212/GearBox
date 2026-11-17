package com.jaguar.gearbox.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ToolExamplesTest {
    @Test fun examplesAreBoundedInputOnlyAndHaveHelp() {
        assertEquals(33, ToolExampleCatalog.routes.size)
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
        listOf(Tools.ROUTE_IMAGE_STUDIO, Tools.ROUTE_PASSWORD_GENERATOR, Tools.ROUTE_TIMER_STOPWATCH, Tools.ROUTE_DICE,
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
    @Test fun billSplitterExampleContainsOnlyValidatedDraftInputs() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_BILL_SPLITTER)).inputs
        assertEquals(setOf("bill", "tax", "tipPercent", "diners"), inputs.keys)
        assertNotNull(parseBillSplitterSnapshot(inputs))
    }
    @Test fun businessDayExampleContainsOnlyValidatedDraftInputs() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_BUSINESS_DAY_PLANNER)).inputs
        assertEquals(setOf("mode", "start", "end", "offset", "weekendMask", "excludedDates"), inputs.keys)
        assertNotNull(parseBusinessDaySnapshot(inputs))
    }
    @Test fun scientificExampleContainsOnlyValidatedExpressionInputs() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_SCIENTIFIC_CALCULATOR)).inputs
        assertEquals(mapOf("expression" to "sqrt(16)+2^3", "angleMode" to "DEG"), inputs)
        assertNotNull(parseScientificSnapshot(inputs))
    }
    @Test fun textDiffExampleContainsOnlyLiteralInputs() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_TEXT_DIFF)).inputs
        assertEquals(mapOf("firstText" to "Hello\nworld", "secondText" to "Hello\nGearBox"), inputs)
        assertTrue(ToolMemoryPolicy.validInputs(inputs))
    }
    @Test fun timeZoneExampleContainsOnlyUnambiguousInputs() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_TIME_ZONE_CONVERTER)).inputs
        assertEquals(mapOf("date" to "2026-01-15", "time" to "12:00", "sourceZone" to "UTC", "targetZone" to "Asia/Kolkata", "sourceOffset" to "AUTO"), inputs)
        assertTrue(ToolMemoryPolicy.validInputs(inputs))
    }
    @Test fun decisionExampleContainsOnlyValidatedGenericInputs() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_DECISION_MATRIX)).inputs
        assertEquals(mapOf("optionLabels" to "Option A\nOption B", "criterionLabels" to "Value\nComfort", "weights" to "1,1", "scores" to "4,3\n3,4"), inputs)
        assertNotNull(parseDecisionMatrixSnapshot(inputs))
    }
    @Test fun aspectExampleContainsOnlyValidatedDimensions() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_ASPECT_RATIO)).inputs
        assertEquals(mapOf("mode" to "WIDTH", "sourceWidth" to "1920", "sourceHeight" to "1080", "targetWidth" to "1280", "targetHeight" to "", "percent" to ""), inputs)
        assertNotNull(parseAspectRatioSnapshot(inputs))
    }
    @Test fun deadlineExampleContainsOnlyValidatedPlanningInputs() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_DEADLINE_PLANNER)).inputs
        assertEquals(mapOf("deadline" to "2027-01-10", "asOf" to "2027-01-01", "ids" to "1,2", "names" to "Prepare\nReview", "durations" to "2,3"), inputs)
        assertNotNull(parseDeadlineSnapshot(inputs))
    }
    @Test fun jsonExampleContainsOnlyValidatedSourceAndSelectors() {
        val inputs = requireNotNull(ToolExampleCatalog.forRoute(Tools.ROUTE_JSON_FORMATTER)).inputs
        assertEquals(mapOf("source" to "{\"name\":\"GearBox\",\"enabled\":true}", "mode" to "PRETTY", "indent" to "2"), inputs)
        assertNotNull(parseJsonFormatterSnapshot(inputs))
    }
}
