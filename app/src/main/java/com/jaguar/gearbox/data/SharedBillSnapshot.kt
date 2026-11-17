package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.*

/** Compact current-input schema, independent of legacy simple bill snapshots. */
data class SharedBillSnapshot(val participants: List<SharedParticipant>, val expenses: List<SharedExpense>) {
    fun toInputs(): Map<String, String> {
        require(validSharedBill(participants, expenses))
        return mapOf("format" to "shared-v1", "view" to "SHARED",
            "participantIds" to participants.joinToString(",") { it.id.toString() },
            "participantNames" to participants.joinToString("\n") { it.label },
            "expenses" to expenses.joinToString("\n") { expense ->
                val mask = participants.foldIndexed(0) { index, mask, p -> if (p.id in expense.beneficiaryIds) mask or (1 shl index) else mask }
                "${expense.id},${expense.amountMinor},${expense.payerId},$mask"
            })
    }
}

/** Exact keys and canonical numbers mean toInputs preserves the original hydration fingerprint. */
fun parseSharedBillSnapshot(values: Map<String, String>): SharedBillSnapshot? {
    if (values.keys != setOf("format", "view", "participantIds", "participantNames", "expenses") ||
        values["format"] != "shared-v1" || values["view"] != "SHARED" || !ToolMemoryPolicy.validInputs(values)) return null
    fun positiveId(value: String): Int? = value.takeIf { Regex("[1-9][0-9]{0,9}").matches(it) }
        ?.toIntOrNull()?.takeIf { it in 1..SHARED_BILL_MAX_ID }
    val idTokens = values.getValue("participantIds").split(',')
    val labels = values.getValue("participantNames").split('\n')
    if (idTokens.size !in 2..10 || labels.size != idTokens.size) return null
    val participants = idTokens.mapIndexed { index, token -> SharedParticipant(positiveId(token) ?: return null, labels[index]) }
    val rows = values.getValue("expenses").split('\n')
    if (rows.size !in 1..20) return null
    val expenses = rows.map { row ->
        val fields = row.split(',')
        if (fields.size != 4) return null
        val id = positiveId(fields[0]) ?: return null
        val amount = fields[1].takeIf { Regex("0|[1-9][0-9]{0,11}").matches(it) }?.toLongOrNull()
            ?.takeIf { it <= SHARED_BILL_MAX_EXPENSE_MINOR } ?: return null
        val payer = positiveId(fields[2]) ?: return null
        val mask = fields[3].takeIf { Regex("[1-9][0-9]{0,3}").matches(it) }?.toIntOrNull()
            ?.takeIf { it < (1 shl participants.size) } ?: return null
        val beneficiaries = participants.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.map { it.id }.toSet()
        SharedExpense(id, amount, payer, beneficiaries)
    }
    return SharedBillSnapshot(participants, expenses).takeIf { validSharedBill(participants, expenses) }
}
