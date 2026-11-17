package com.jaguar.gearbox.logic

import java.math.BigDecimal
import java.math.RoundingMode

private val billDecimal = Regex("(?:0|[1-9][0-9]{0,9})(?:\\.[0-9]{1,2})?")
fun parseBillAmount(text: String): BigDecimal? = parseBillDecimal(text, BigDecimal("1000000000.00"))
fun parseBillPercent(text: String): BigDecimal? = parseBillDecimal(text, BigDecimal("100"))
private fun parseBillDecimal(text: String, maximum: BigDecimal): BigDecimal? =
    if (!billDecimal.matches(text)) null else text.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO && it <= maximum }

data class BillSplitResult(val billMinor: Long, val taxMinor: Long, val tipMinor: Long,
    val totalMinor: Long, val diners: Int, val baseShareMinor: Long, val extraCentDiners: Int)

/** Tip applies to the original pre-tax bill only; round tip HALF_UP once, then split integer cents. */
fun calculateBillSplit(bill: BigDecimal, tax: BigDecimal, tipPercent: BigDecimal, diners: Int): BillSplitResult {
    require(diners in 1..1000)
    require(bill >= BigDecimal.ZERO && bill <= BigDecimal("1000000000.00") && bill.stripTrailingZeros().scale() <= 2)
    require(tax >= BigDecimal.ZERO && tax <= BigDecimal("1000000000.00") && tax.stripTrailingZeros().scale() <= 2)
    require(tipPercent >= BigDecimal.ZERO && tipPercent <= BigDecimal("100") && tipPercent.stripTrailingZeros().scale() <= 2)
    val billMinor = bill.movePointRight(2).longValueExact()
    val taxMinor = tax.movePointRight(2).longValueExact()
    val tipMinor = bill.multiply(tipPercent).divide(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    val total = billMinor + taxMinor + tipMinor
    return BillSplitResult(billMinor, taxMinor, tipMinor, total, diners, total / diners, (total % diners).toInt())
}

/** Plain exact two-place monetary text, without a currency symbol or binary floating point. */
fun billMinorText(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()
