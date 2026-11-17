package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class FocusCatalogPolicyTest {
    @Test fun liveSessionIsExcludedFromGenericCaptureAndHandoffs() {
        val route = "tool/focus_timer"
        assertFalse(ToolMemoryPolicy.permitsRoute(route))
        val settings = ToolMemorySettings(historyEnabled = true, rememberModes = true, rememberResults = true)
        assertFalse(settings.permitsHistory(route))
        assertFalse(settings.permitsResult(route))
        assertTrue(compatibleToolHandoffs(route, mapOf("input" to "12"), "12").isEmpty())
    }
    @Test fun discoveryDoesNotAutomaticallySelectOrStartSession() {
        val matches = ToolSearchEngine.rank("pomodoro timer", ToolSearchCatalog.englishDocuments())
        assertEquals("tool/focus_timer", matches.first().route)
        assertNull(matches.first().operationId)
        assertNull(ToolExampleCatalog.forRoute("tool/focus_timer"))
    }
}
