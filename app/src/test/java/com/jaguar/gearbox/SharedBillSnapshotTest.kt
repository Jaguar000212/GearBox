package com.jaguar.gearbox

import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class SharedBillSnapshotTest {
    private val model = SharedBillSnapshot(listOf(SharedParticipant(10, " A "), SharedParticipant(20, "B"), SharedParticipant(30, "C")),
        listOf(SharedExpense(1, 101, 10, setOf(30))))
    @Test fun roundTripKeepsIdsNamesAndCanonicalCents() {
        val inputs = model.toInputs()
        assertEquals("SHARED", inputs["view"])
        assertEquals(model, parseSharedBillSnapshot(inputs))
        assertEquals(inputs, parseSharedBillSnapshot(inputs)!!.toInputs())
        val removedUnreferenced = model.copy(participants = model.participants.filterNot { it.id == 20 })
        assertEquals(setOf(30), parseSharedBillSnapshot(removedUnreferenced.toInputs())!!.expenses.single().beneficiaryIds)
        assertEquals("1,101,10,2", removedUnreferenced.toInputs()["expenses"])
    }
    @Test fun strictVersionIdsMasksAndPayloadBoundsRejectMalformedSnapshots() {
        val valid = model.toInputs()
        for ((key, value) in listOf("format" to "shared-v2", "view" to "SIMPLE", "participantIds" to "10,10,30",
            "participantIds" to "010,20,30", "participantNames" to "A\nB", "participantNames" to "A\u2028Z\nB\nC",
            "expenses" to "1,101,99,4", "expenses" to "1,101,10,0", "expenses" to "1,101,10,8",
            "expenses" to "1,0101,10,4", "expenses" to "1,100000000001,10,4",
            "expenses" to "1,101,10,4\n1,20,20,1")) assertNull(parseSharedBillSnapshot(valid + (key to value)))
        assertNull(parseSharedBillSnapshot(valid + ("contacts" to "not stored")))
        assertNull(parseSharedBillSnapshot(valid - "view"))
    }
    @Test fun maximumSchemaFitsExistingStorageWithoutAnyLimitExpansion() {
        val participants = (1..10).map { SharedParticipant(it, "x".repeat(60)) }
        val expenses = (1..20).map { SharedExpense(it, SHARED_BILL_MAX_EXPENSE_MINOR, 1, participants.map { it.id }.toSet()) }
        val snapshot = SharedBillSnapshot(participants, expenses)
        assertEquals(snapshot, parseSharedBillSnapshot(snapshot.toInputs()))
        assertTrue(ToolMemoryPolicy.validRecord(snapshot.toInputs(), "summary"))
    }
    @Test fun legacySimpleRemainsValidButBothViewsRequirePrivateCaptureConsent() {
        assertNotNull(parseBillSplitterSnapshot(mapOf("bill" to "100", "tax" to "0", "tipPercent" to "10", "diners" to "3")))
        val route = "tool/bill_splitter"
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("diners" to "3")))
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("view" to "SIMPLE", "diners" to "3")))
        assertTrue(ToolMemoryPolicy.validMode(route, mapOf("view" to "SHARED")))
        assertFalse(ToolMemoryPolicy.validMode(route, model.toInputs()))
        assertFalse(ToolMemoryPolicy.validMode(route, mapOf("view" to "SHARED", "diners" to "3")))
        assertFalse(ToolMemorySettings(historyEnabled = true, rememberResults = true).permitsResult(route))
    }
}
