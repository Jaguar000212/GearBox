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
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.parseBillSplitterSnapshot
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*

@Composable
fun BillSplitterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var bill by rememberSaveable { mutableStateOf("") }
    var tax by rememberSaveable { mutableStateOf("0") }
    var tip by rememberSaveable { mutableStateOf("0") }
    var diners by rememberSaveable { mutableStateOf("2") }
    var result by remember { mutableStateOf<BillSplitResult?>(null) }
    var restoreError by remember { mutableStateOf(false) }
    val inputs = mapOf("bill" to bill, "tax" to tax, "tipPercent" to tip, "diners" to diners)
    val setup = parseBillSplitterSnapshot(inputs)
    val dinerCount = diners.takeIf { Regex("[1-9][0-9]{0,3}").matches(it) }?.toIntOrNull()?.takeIf { it in 1..1000 }
    fun edited() { result = null; restoreError = false }
    fun restore(values: Map<String, String>, completed: Boolean = false) {
        val saved = parseBillSplitterSnapshot(values)
        if (saved == null) { restoreError = true; return }
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
    val output = if (totals != null && shares != null) context.getString(R.string.bill_split_export, totals, shares) else null
    val history = rememberToolHistory(route = Tools.ROUTE_BILL_SPLITTER, inputs = inputs, output = output,
        modeInputs = if (dinerCount != null) mapOf("diners" to diners) else emptyMap(),
        onRestore = { restore(it) }, onRestoreLastResult = { restore(it, completed = true) },
        onRestoreMode = { values ->
            val value = values["diners"]
            if (value != null && Regex("[1-9][0-9]{0,3}").matches(value) && value.toInt() in 1..1000) {
                diners = value; edited()
            }
        }, resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false })
    ToolScaffold(stringResource(R.string.bill_split_title), Tools.byRoute(Tools.ROUTE_BILL_SPLITTER)!!.icon,
        onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
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
        }, result = {
            Text(stringResource(R.string.bill_split_output_heading), style = MaterialTheme.typography.titleLarge)
            if (output != null) {
                ResultCard(output)
                Text(stringResource(R.string.bill_split_remainder_note), style = MaterialTheme.typography.bodySmall)
            } else Text(stringResource(R.string.bill_split_empty))
            ToolActions(onCopy = { output?.let { context.copyToClipboard(context.getString(R.string.bill_split_title), it) } },
                onShare = { output?.let { context.shareText(it) } },
                onClear = { bill = ""; tax = "0"; tip = "0"; edited() },
                resultEnabled = output != null, clearEnabled = bill.isNotEmpty() || tax != "0" || tip != "0" || result != null)
        })
    }
}

@Composable
private fun BillField(value: String, onChange: (String) -> Unit, label: Int, invalid: Boolean, error: Int, integer: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = true,
        isError = invalid, supportingText = { if (invalid) Text(stringResource(error)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}
