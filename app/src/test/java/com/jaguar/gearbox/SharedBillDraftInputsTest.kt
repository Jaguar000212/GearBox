package com.jaguar.gearbox

import com.jaguar.gearbox.ui.tools.sharedBillDraftInputs
import com.jaguar.gearbox.data.parseSharedBillSnapshot
import org.junit.Assert.*
import org.junit.Test

class SharedBillDraftInputsTest {
    @Test fun everyRawEditorChangeInvalidatesPendingHydrationEquality() {
        val initial = sharedBillDraftInputs(listOf(1, 2), listOf("A", "B"), listOf(7), listOf(""), listOf(1), listOf("1,2"))
        val changes = listOf(
            sharedBillDraftInputs(listOf(1, 3), listOf("A", "B"), listOf(7), listOf(""), listOf(1), listOf("1,2")),
            sharedBillDraftInputs(listOf(1, 2), listOf("A\n", "B"), listOf(7), listOf(""), listOf(1), listOf("1,2")),
            sharedBillDraftInputs(listOf(1, 2), listOf("A", "B"), listOf(8), listOf(""), listOf(1), listOf("1,2")),
            sharedBillDraftInputs(listOf(1, 2), listOf("A", "B"), listOf(7), listOf("invalid"), listOf(1), listOf("1,2")),
            sharedBillDraftInputs(listOf(1, 2), listOf("A", "B"), listOf(7), listOf(""), listOf(2), listOf("1,2")),
            sharedBillDraftInputs(listOf(1, 2), listOf("A", "B"), listOf(7), listOf(""), listOf(1), listOf("")),
        )
        changes.forEach { assertNotEquals(initial, it); assertNull(parseSharedBillSnapshot(it)) }
        assertEquals(initial, sharedBillDraftInputs(listOf(1, 2), listOf("A", "B"), listOf(7), listOf(""), listOf(1), listOf("1,2")))
    }
    @Test fun boundedFingerprintDoesNotEmbedPrivateDraftAndSeparatorsCannotCollide() {
        val a = sharedBillDraftInputs(listOf(1, 2), listOf("A|B", "C"), listOf(7), listOf("private".repeat(1000)), listOf(1), listOf("1,2"))
        val b = sharedBillDraftInputs(listOf(1, 2), listOf("A", "B|C"), listOf(7), listOf("private".repeat(1000)), listOf(1), listOf("1,2"))
        assertNotEquals(a, b)
        assertEquals(64, a.getValue("draftFingerprint").length)
        assertFalse(a.values.any { it.contains("private") })
    }
}
