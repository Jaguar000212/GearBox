package com.jaguar.gearbox.logic

const val SHARED_BILL_MAX_ID = 1_000_000_000
const val SHARED_BILL_MAX_EXPENSE_MINOR = 100_000_000_000L

data class SharedParticipant(val id: Int, val label: String)
data class SharedExpense(val id: Int, val amountMinor: Long, val payerId: Int, val beneficiaryIds: Set<Int>)
data class SharedBalance(val participantId: Int, val paidMinor: Long, val owedMinor: Long, val netMinor: Long)
data class SharedSettlement(val fromId: Int, val toId: Int, val amountMinor: Long)
data class SharedBillResult(val balances: List<SharedBalance>, val settlements: List<SharedSettlement>, val totalMinor: Long)

fun validSharedParticipantLabel(label: String): Boolean = label.isNotBlank() && label.length <= 60 &&
    label.none { it.isISOControl() || it == '\u2028' || it == '\u2029' }

fun validSharedBill(participants: List<SharedParticipant>, expenses: List<SharedExpense>): Boolean {
    if (participants.size !in 2..10 || expenses.size !in 1..20) return false
    if (participants.any { it.id !in 1..SHARED_BILL_MAX_ID || !validSharedParticipantLabel(it.label) }) return false
    val ids = participants.map { it.id }.toSet()
    if (ids.size != participants.size || expenses.map { it.id }.toSet().size != expenses.size) return false
    return expenses.all { it.id in 1..SHARED_BILL_MAX_ID && it.amountMinor in 0..SHARED_BILL_MAX_EXPENSE_MINOR &&
        it.payerId in ids && it.beneficiaryIds.isNotEmpty() && it.beneficiaryIds.all(ids::contains) }
}

/** Exact cents. Each expense's remainder goes to its earliest beneficiaries in participant order.
 * Settlement suggestions use deterministic participant-order greedy matching, not minimum transfers.
 * Payers need not be beneficiaries. No money is transferred and no external information is consulted.
 */
fun calculateSharedBill(participants: List<SharedParticipant>, expenses: List<SharedExpense>): SharedBillResult {
    require(validSharedBill(participants, expenses)) { "Invalid shared bill" }
    val paid = participants.associate { it.id to 0L }.toMutableMap()
    val owed = participants.associate { it.id to 0L }.toMutableMap()
    for (expense in expenses) {
        paid[expense.payerId] = paid.getValue(expense.payerId) + expense.amountMinor
        val beneficiaries = participants.filter { it.id in expense.beneficiaryIds }
        val base = expense.amountMinor / beneficiaries.size
        val remainder = expense.amountMinor % beneficiaries.size
        beneficiaries.forEachIndexed { index, participant ->
            owed[participant.id] = owed.getValue(participant.id) + base + if (index < remainder) 1L else 0L
        }
    }
    val balances = participants.map { SharedBalance(it.id, paid.getValue(it.id), owed.getValue(it.id), paid.getValue(it.id) - owed.getValue(it.id)) }
    val debts = balances.filter { it.netMinor < 0 }.map { it.participantId to -it.netMinor }.toMutableList()
    val credits = balances.filter { it.netMinor > 0 }.map { it.participantId to it.netMinor }.toMutableList()
    val settlements = mutableListOf<SharedSettlement>()
    var debtor = 0
    var creditor = 0
    while (debtor < debts.size && creditor < credits.size) {
        val (from, debt) = debts[debtor]
        val (creditorId, credit) = credits[creditor]
        val amount = minOf(debt, credit)
        settlements += SharedSettlement(from, creditorId, amount)
        debts[debtor] = from to (debt - amount)
        credits[creditor] = creditorId to (credit - amount)
        if (debts[debtor].second == 0L) debtor++
        if (credits[creditor].second == 0L) creditor++
    }
    return SharedBillResult(balances, settlements, expenses.sumOf { it.amountMinor })
}
