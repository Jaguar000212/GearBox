package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test

class ImportedRetentionPolicyTest {
    @Test fun liveImportedOutputRemainsSuppressedAcrossRecompositions() {
        repeat(3) { assertTrue(suppressImportedObservation("same", "same", false, true)) }
        assertTrue(suppressImportedObservation("same", "same", false, false))
    }
    @Test fun userInputOrConfigurationChangeReleasesGate() {
        assertFalse(suppressImportedObservation("old", "edited", false, true))
        assertFalse(suppressImportedObservation("old", "edited", false, false))
    }
    @Test fun explicitGenerationReleasesOnlyExplicitActionTools() {
        assertTrue(suppressImportedObservation("same", "same", true, false))
        assertFalse(suppressImportedObservation("same", "same", true, true))
        assertTrue(suppressImportedObservation("same", "same", false, true))
    }
    @Test fun normalImportsRemainUnaffectedAndDerivedMetadataIsIgnored() {
        assertFalse(suppressImportedObservation(null, "same", false, true))
        val inputs = mapOf("input" to "text", "mode" to "TEXT_TO_MORSE")
        assertEquals(inputs, retentionInputValues(inputs + ("_partial" to "true")))
        assertNotEquals(retentionInputValues(inputs), retentionInputValues(inputs + ("mode" to "TEXT_TO_BINARY")))
    }
}
