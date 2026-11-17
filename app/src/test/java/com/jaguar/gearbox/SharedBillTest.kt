package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class SharedBillTest {
    private val people = listOf(SharedParticipant(10, "A"), SharedParticipant(20, "B"), SharedParticipant(30, "C"))
    @Test fun perExpenseRemainderUsesParticipantOrderNotSetIteration() {
        val result = calculateSharedBill(people, listOf(SharedExpense(1, 100, 10, linkedSetOf(30, 20, 10))))
        assertEquals(listOf(34L, 33L, 33L), result.balances.map { it.owedMinor })
        assertEquals(listOf(66L, -33L, -33L), result.balances.map { it.netMinor })
        assertEquals(listOf(SharedSettlement(20, 10, 33), SharedSettlement(30, 10, 33)), result.settlements)
    }
    @Test fun payerCanBeOutsideBeneficiarySetAndMultipleExpensesConserveCents() {
        val result = calculateSharedBill(people, listOf(SharedExpense(1, 101, 10, setOf(20, 30)),
            SharedExpense(2, 40, 20, setOf(10, 30))))
        assertEquals(listOf(20L, 51L, 70L), result.balances.map { it.owedMinor })
        assertEquals(141L, result.totalMinor)
        assertEquals(0L, result.balances.sumOf { it.netMinor })
        val after = result.balances.associate { it.participantId to it.netMinor }.toMutableMap()
        result.settlements.forEach { transfer ->
            assertTrue(transfer.amountMinor > 0)
            after[transfer.fromId] = after.getValue(transfer.fromId) + transfer.amountMinor
            after[transfer.toId] = after.getValue(transfer.toId) - transfer.amountMinor
        }
        assertTrue(after.values.all { it == 0L })
    }
    @Test fun balancedAndZeroBillsNeedNoTransfersAndMaximumDoesNotOverflow() {
        val own = people.mapIndexed { index, p -> SharedExpense(index + 1, 100, p.id, setOf(p.id)) }
        assertTrue(calculateSharedBill(people, own).settlements.isEmpty())
        assertTrue(calculateSharedBill(people, listOf(SharedExpense(1, 0, 10, setOf(10, 20)))).settlements.isEmpty())
        val maximum = calculateSharedBill(people, (1..20).map { SharedExpense(it, SHARED_BILL_MAX_EXPENSE_MINOR, 10, setOf(20, 30)) })
        assertEquals(2_000_000_000_000L, maximum.totalMinor)
        assertEquals(0L, maximum.balances.sumOf { it.netMinor })
    }
    @Test fun staleUnknownAndDuplicateReferencesFailClosed() {
        val expense = SharedExpense(1, 100, 10, setOf(20))
        for (bad in listOf(expense.copy(payerId = 99), expense.copy(beneficiaryIds = setOf(99)),
            expense.copy(beneficiaryIds = emptySet()), expense.copy(amountMinor = -1), expense.copy(amountMinor = SHARED_BILL_MAX_EXPENSE_MINOR + 1)))
            assertFalse(validSharedBill(people, listOf(bad)))
        assertFalse(validSharedBill(people, listOf(expense, expense)))
        assertFalse(validSharedBill(listOf(people[0], people[0]), listOf(expense)))
        assertFalse(validSharedBill(people.filterNot { it.id == 20 }, listOf(expense)))
        assertThrows(IllegalArgumentException::class.java) { calculateSharedBill(people, listOf(expense.copy(payerId = 99))) }
    }
}
