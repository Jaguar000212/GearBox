package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.parseBillAmount
import com.jaguar.gearbox.logic.parseBillPercent

/** Validated completed-run inputs. Raw decimal spelling is retained for hydration fingerprints. */
data class BillSplitterSnapshot(val bill: String, val tax: String, val tipPercent: String, val diners: Int) {
    fun toInputs(): Map<String, String> = mapOf("bill" to bill, "tax" to tax, "tipPercent" to tipPercent, "diners" to diners.toString())
}

fun parseBillSplitterDiners(value: String?): Int? {
    if (value == null || !Regex("[1-9][0-9]{0,3}").matches(value)) return null
    return value.toIntOrNull()?.takeIf { it in 1..1000 }
}

/** Tax is a decimal amount; tip is a percent of the original bill. Never infer extra schema keys. */
fun parseBillSplitterSnapshot(values: Map<String, String>): BillSplitterSnapshot? {
    if (values.keys != setOf("bill", "tax", "tipPercent", "diners")) return null
    val bill = values.getValue("bill")
    val tax = values.getValue("tax")
    val tipPercent = values.getValue("tipPercent")
    if (parseBillAmount(bill) == null || parseBillAmount(tax) == null || parseBillPercent(tipPercent) == null) return null
    val diners = parseBillSplitterDiners(values["diners"]) ?: return null
    return BillSplitterSnapshot(bill, tax, tipPercent, diners)
}

/** Remembering modes/presets cannot quietly retain an old bill, tax or tip amount. */
fun validBillSplitterMode(values: Map<String, String>): Boolean =
    values.keys == setOf("diners") && parseBillSplitterDiners(values["diners"]) != null
