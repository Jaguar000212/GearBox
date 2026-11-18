package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*

private data class SharedBillRemoval(val participant: Boolean, val id: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillSplitterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var view by rememberSaveable { mutableStateOf("SIMPLE") }
    var participantIds by rememberSaveable { mutableStateOf(arrayListOf(1, 2)) }
    var participantNames by rememberSaveable { mutableStateOf(arrayListOf(context.getString(R.string.shared_bill_person, 1), context.getString(R.string.shared_bill_person, 2))) }
    var expenseIds by rememberSaveable { mutableStateOf(arrayListOf(1)) }
    var amounts by rememberSaveable { mutableStateOf(arrayListOf("")) }
    var payers by rememberSaveable { mutableStateOf(arrayListOf(1)) }
    var beneficiaries by rememberSaveable { mutableStateOf(arrayListOf("1,2")) }
    var nextParticipant by rememberSaveable { mutableIntStateOf(3) }
    var nextExpense by rememberSaveable { mutableIntStateOf(2) }
    var sharedResult by remember { mutableStateOf<SharedBillResult?>(null) }
    var removal by remember { mutableStateOf<SharedBillRemoval?>(null) }
    var shareReview by remember { mutableStateOf<Pair<Map<String, String>, String>?>(null) }
    var bill by rememberSaveable { mutableStateOf("") }
    var tax by rememberSaveable { mutableStateOf("0") }
    var tip by rememberSaveable { mutableStateOf("0") }
    var diners by rememberSaveable { mutableStateOf("2") }
    var result by remember { mutableStateOf<BillSplitResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    val simpleInputs = mapOf("bill" to bill, "tax" to tax, "tipPercent" to tip, "diners" to diners)
    val setup = parseBillSplitterSnapshot(simpleInputs)
    val dinerCount = diners.takeIf { Regex("[1-9][0-9]{0,3}").matches(it) }?.toIntOrNull()?.takeIf { it in 1..1000 }
    fun edited() { result = null; sharedResult = null; restoreError = false; removal = null; shareReview = null }
    fun currentShared(): SharedBillSnapshot? {
        if (participantIds.size != participantNames.size || expenseIds.size != amounts.size || payers.size != amounts.size || beneficiaries.size != amounts.size) return null
        val participants = participantIds.mapIndexed { index, id -> SharedParticipant(id, participantNames[index]) }
        val expenses = expenseIds.mapIndexed { index, id ->
            val amount = parseBillAmount(amounts[index]) ?: return null
            val selected = if (beneficiaries[index].isEmpty()) emptySet() else beneficiaries[index].split(',').map { it.toIntOrNull() ?: return null }.toSet()
            SharedExpense(id, amount.movePointRight(2).longValueExact(), payers[index], selected)
        }
        if (!validSharedBill(participants, expenses)) return null
        return SharedBillSnapshot(participants, expenses)
    }
    val sharedSetup = currentShared()
    val inputs = if (view == "SHARED") sharedSetup?.toInputs() ?: sharedBillDraftInputs(participantIds, participantNames, expenseIds, amounts, payers, beneficiaries) else simpleInputs
    fun isReferenced(id: Int) = payers.contains(id) || beneficiaries.any { list -> list.split(',').any { it.toIntOrNull() == id } }
    fun personName(id: Int): String = participantIds.indexOf(id).takeIf { it >= 0 }?.let { participantNames[it] }.orEmpty()

    fun restore(values: Map<String, String>, completed: Boolean = false) {
        if (values["format"] == "shared-v1") {
            val saved = parseSharedBillSnapshot(values)
            if (saved == null) { restoreError = true; return }
            view = "SHARED"
            participantIds = ArrayList(saved.participants.map { it.id }); participantNames = ArrayList(saved.participants.map { it.label })
            expenseIds = ArrayList(saved.expenses.map { it.id }); amounts = ArrayList(saved.expenses.map { billMinorText(it.amountMinor) })
            payers = ArrayList(saved.expenses.map { it.payerId })
            beneficiaries = ArrayList(saved.expenses.map { expense -> saved.participants.filter { it.id in expense.beneficiaryIds }.joinToString(",") { it.id.toString() } })
            nextParticipant = (participantIds.maxOrNull() ?: 0) + 1; nextExpense = (expenseIds.maxOrNull() ?: 0) + 1
            edited()
            if (completed) sharedResult = calculateSharedBill(saved.participants, saved.expenses)
            return
        }
        val saved = parseBillSplitterSnapshot(values)
        if (saved == null) { restoreError = true; return }
        view = "SIMPLE"
        edited()
        // Preserve exact validated numeric spelling to match remembered-result fingerprints.
        bill = values.getValue("bill"); tax = values.getValue("tax")
        tip = values.getValue("tipPercent"); diners = values.getValue("diners")
        result = if (completed) calculateBillSplit(requireNotNull(parseBillAmount(bill)),
            requireNotNull(parseBillAmount(tax)), requireNotNull(parseBillPercent(tip)), saved.diners) else null
        restoreError = false
    }
    val totals = result?.let { context.getString(R.string.bill_split_totals, billMinorText(it.billMinor),
        billMinorText(it.taxMinor), billMinorText(it.tipMinor), billMinorText(it.totalMinor)) }
    val shares = result?.let { value ->
        val extra = value.extraCentDiners
        if (extra == 0) context.getString(R.string.bill_split_equal, value.diners, billMinorText(value.baseShareMinor))
        else {
            val first = if (extra == 1) context.getString(R.string.bill_split_first_one, billMinorText(value.baseShareMinor + 1))
                else context.getString(R.string.bill_split_first_many, extra, billMinorText(value.baseShareMinor + 1))
            val remaining = if (value.diners - extra == 1) context.getString(R.string.bill_split_remaining_one,
                value.diners, billMinorText(value.baseShareMinor)) else context.getString(R.string.bill_split_remaining_many,
                extra + 1, value.diners, billMinorText(value.baseShareMinor))
            "$first\n$remaining"
        }
    }
    val simpleOutput = if (totals != null && shares != null) context.getString(R.string.bill_split_export, totals, shares) else null
    val sharedOutput = sharedResult?.let { calculated ->
        val balances = calculated.balances.joinToString("\n\n") { row -> context.getString(R.string.shared_bill_balance,
            row.participantId, personName(row.participantId), billMinorText(row.paidMinor), billMinorText(row.owedMinor), billMinorText(row.netMinor)) }
        val transfers = if (calculated.settlements.isEmpty()) context.getString(R.string.shared_bill_settled) else calculated.settlements.joinToString("\n") { transfer ->
            context.getString(R.string.shared_bill_transfer, transfer.fromId, personName(transfer.fromId), transfer.toId, personName(transfer.toId), billMinorText(transfer.amountMinor)) }
        context.getString(R.string.shared_bill_output, context.getString(R.string.shared_bill_total, billMinorText(calculated.totalMinor)),
            balances, transfers, context.getString(R.string.shared_bill_settlement_note))
    }
    val output = if (view == "SHARED") sharedOutput else simpleOutput
    val history = rememberToolHistory(route = Tools.ROUTE_BILL_SPLITTER, inputs = inputs, output = output,
        modeInputs = if (view == "SHARED") mapOf("view" to "SHARED") else if (dinerCount != null) mapOf("diners" to diners) else emptyMap(),
        onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        onRestoreMode = { values ->
            if (validBillSplitterMode(values)) {
                if (values["view"] == "SHARED") { view = "SHARED"; edited() }
                else values["diners"]?.let { view = "SIMPLE"; diners = it; edited() }
            }
        }, resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false },
        canResetHydratedCaptureOnRestore = { values ->
            parseSharedBillSnapshot(values) != null || parseBillSplitterSnapshot(values) != null
        },
        restoredModeInputs = { values ->
            if (values["format"] == "shared-v1") mapOf("view" to "SHARED")
            else values["diners"]?.let { mapOf("diners" to it) }.orEmpty()
        })
    ToolScaffold(stringResource(R.string.bill_split_title), Tools.byRoute(Tools.ROUTE_BILL_SPLITTER)!!.icon,
        onNavigateBack, history = history) {
        CompactChoiceSelector(stringResource(R.string.shared_bill_view), view, listOf("SIMPLE", "SHARED"),
            { context.getString(if (it == "SIMPLE") R.string.shared_bill_simple else R.string.shared_bill_shared) }, { view = it; edited() })
        AdaptiveToolLayout(input = {
            if (view == "SIMPLE") {
            BillField(bill, { bill = it; edited() }, R.string.bill_split_bill, parseBillAmount(bill) == null, R.string.bill_split_amount_error)
            BillField(tax, { tax = it; edited() }, R.string.bill_split_tax, parseBillAmount(tax) == null, R.string.bill_split_amount_error)
            BillField(tip, { tip = it; edited() }, R.string.bill_split_tip, parseBillPercent(tip) == null, R.string.bill_split_tip_error)
            BillField(diners, { diners = it; edited() }, R.string.bill_split_diners, dinerCount == null, R.string.bill_split_diners_error, integer = true)
            Button(enabled = setup != null, onClick = {
                setup?.let { result = calculateBillSplit(requireNotNull(parseBillAmount(bill)),
                    requireNotNull(parseBillAmount(tax)), requireNotNull(parseBillPercent(tip)), it.diners); restoreError = false }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.bill_split_calculate)) }
            if (restoreError) Text(stringResource(R.string.bill_split_restore_error), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.bill_split_units), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.bill_split_math), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.bill_split_restore_note), style = MaterialTheme.typography.bodySmall)
            } else {
                Text(stringResource(R.string.shared_bill_participants), style = MaterialTheme.typography.titleLarge)
                participantIds.forEachIndexed { index, id ->
                    OutlinedTextField(participantNames[index], { participantNames = ArrayList(participantNames).also { list -> list[index] = it }; edited() },
                        label = { Text(stringResource(R.string.shared_bill_person_label, id)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    val referenced = isReferenced(id)
                    TextButton(enabled = participantIds.size > 2 && !referenced, onClick = { removal = SharedBillRemoval(true, id) }) {
                        Text(stringResource(R.string.shared_bill_remove_person, id))
                    }
                    if (referenced && participantIds.size > 2) Text(stringResource(R.string.shared_bill_referenced), style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(enabled = participantIds.size < 10 && nextParticipant <= 1000000000, onClick = {
                    val id = nextParticipant++
                    participantIds = ArrayList(participantIds + id); participantNames = ArrayList(participantNames + context.getString(R.string.shared_bill_person, id)); edited()
                }) { Text(stringResource(R.string.shared_bill_add_person)) }
                Text(stringResource(R.string.shared_bill_expenses), style = MaterialTheme.typography.titleLarge)
                expenseIds.forEachIndexed { index, id ->
                    OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.shared_bill_expense, id), style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(amounts[index], { amounts = ArrayList(amounts).also { list -> list[index] = it }; edited() },
                                label = { Text(stringResource(R.string.shared_bill_amount, id)) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                            CompactChoiceSelector(stringResource(R.string.shared_bill_payer, id), payers[index], participantIds,
                                { context.getString(R.string.shared_bill_person_display, it, personName(it)) },
                                { payers = ArrayList(payers).also { list -> list[index] = it }; edited() })
                            Text(stringResource(R.string.shared_bill_beneficiaries, id))
                            val selected = beneficiaries[index].split(',').mapNotNull(String::toIntOrNull).toSet()
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                participantIds.forEach { participant ->
                                    FilterChip(selected = participant in selected, onClick = {
                                        val changed = if (participant in selected) selected - participant else selected + participant
                                        beneficiaries = ArrayList(beneficiaries).also { list -> list[index] = participantIds.filter { it in changed }.joinToString(",") }
                                        edited()
                                    }, label = { Text(context.getString(R.string.shared_bill_person_display, participant, personName(participant))) })
                                }
                            }
                            TextButton(enabled = expenseIds.size > 1, onClick = { removal = SharedBillRemoval(false, id) }) { Text(stringResource(R.string.shared_bill_remove_expense, id)) }
                        }
                    }
                }
                OutlinedButton(enabled = expenseIds.size < 20 && nextExpense <= 1000000000, onClick = {
                    expenseIds = ArrayList(expenseIds + nextExpense++); amounts = ArrayList(amounts + "")
                    payers = ArrayList(payers + participantIds.first()); beneficiaries = ArrayList(beneficiaries + participantIds.joinToString(",")); edited()
                }) { Text(stringResource(R.string.shared_bill_add_expense)) }
                if (sharedSetup == null) Text(stringResource(R.string.shared_bill_invalid), color = MaterialTheme.colorScheme.error)
                Button(enabled = sharedSetup != null, onClick = {
                    currentShared()?.let { sharedResult = calculateSharedBill(it.participants, it.expenses); restoreError = false }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.shared_bill_calculate)) }
                if (restoreError) Text(stringResource(R.string.bill_split_restore_error), color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.shared_bill_rules), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.bill_split_restore_note), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.shared_bill_privacy), style = MaterialTheme.typography.bodySmall)
        }, result = {
            Text(stringResource(R.string.bill_split_output_heading), style = MaterialTheme.typography.titleLarge)
            if (output != null) {
                ResultCard(output, selectable = false)
                if (view == "SIMPLE") Text(stringResource(R.string.bill_split_remainder_note), style = MaterialTheme.typography.bodySmall)
                if (!ToolMemoryPolicy.validRecord(inputs, output)) Text(stringResource(R.string.shared_bill_not_saved), color = MaterialTheme.colorScheme.error)
            } else Text(stringResource(if (view == "SIMPLE") R.string.bill_split_empty else R.string.shared_bill_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.bill_split_title), it, sensitive = true) } },
                onShare = { output?.let { shareReview = inputs to it } },
                onClear = if (view == "SIMPLE") ({ bill = ""; tax = "0"; tip = "0"; edited() }) else null,
                resultEnabled = output != null, clearEnabled = bill.isNotEmpty() || tax != "0" || tip != "0" || result != null)
        })
    }
    removal?.let { request ->
        AlertDialog(onDismissRequest = { removal = null }, title = { Text(stringResource(R.string.shared_bill_remove_title)) },
            text = { Text(stringResource(R.string.shared_bill_remove_message)) },
            confirmButton = { TextButton(onClick = {
                if (removal === request) {
                    if (request.participant && participantIds.size > 2 && !isReferenced(request.id)) {
                        val index = participantIds.indexOf(request.id)
                        if (index >= 0) {
                            participantIds = ArrayList(participantIds).also { it.removeAt(index) }
                            participantNames = ArrayList(participantNames).also { it.removeAt(index) }
                        }
                    } else if (!request.participant && expenseIds.size > 1) {
                        val index = expenseIds.indexOf(request.id)
                        if (index >= 0) {
                            expenseIds = ArrayList(expenseIds).also { it.removeAt(index) }; amounts = ArrayList(amounts).also { it.removeAt(index) }
                            payers = ArrayList(payers).also { it.removeAt(index) }; beneficiaries = ArrayList(beneficiaries).also { it.removeAt(index) }
                        }
                    }
                    edited()
                }
            }) { Text(stringResource(R.string.shared_bill_remove)) } },
            dismissButton = { TextButton(onClick = { removal = null }) { Text(stringResource(R.string.shared_bill_cancel)) } })
    }
    shareReview?.let { snapshot ->
        if (snapshot.first != inputs || snapshot.second != output) LaunchedEffect(snapshot) { shareReview = null }
        else SensitiveTextShareDialog(snapshot.second, onConfirm = {
            val current = shareReview == snapshot && (result != null || sharedResult != null) && snapshot.first == inputs && snapshot.second == output
            shareReview = null
            if (current) context.shareText(snapshot.second)
        }, onDismiss = { shareReview = null })
    }
}

@Composable
private fun BillField(value: String, onChange: (String) -> Unit, label: Int, invalid: Boolean, error: Int, integer: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true,
        isError = invalid, supportingText = { if (invalid) Text(stringResource(error)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}
