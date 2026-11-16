package com.jaguar.gearbox.ui.tools

import android.content.Context
import com.jaguar.gearbox.R
import com.jaguar.gearbox.logic.PercentageOperation
import com.jaguar.gearbox.logic.calculatePercentage
import com.jaguar.gearbox.logic.calculateGst
import java.math.BigDecimal

// Round-trip decimal notation preserves parsed Double values; final result cards may round more.
private fun n(value: Double): String = value.toString().removeSuffix(".0")

internal fun percentageWorking(context: Context, operation: PercentageOperation, a: Double, b: Double): List<String> {
    val result = calculatePercentage(operation, a, b) ?: return emptyList()
    return when (operation) {
        PercentageOperation.OF_VALUE -> listOf(context.getString(R.string.working_percent_of, n(a), n(a / 100), n(b), n(result)))
        PercentageOperation.WHAT_PERCENT -> listOf(context.getString(R.string.working_what_percent, n(a), n(b), n(a / b), n(result)))
        PercentageOperation.CHANGE -> listOf(context.getString(R.string.working_change, n(b), n(a), n(b - a), n((b - a) / a), n(result)), context.getString(R.string.working_signed_base))
    }
}

internal fun tipWorking(context: Context, bill: Double, rate: Double, people: Int): List<String> {
    if (!bill.isFinite() || !rate.isFinite() || bill < 0 || rate < 0 || people <= 0) return emptyList()
    val tip = bill * (rate / 100)
    val total = bill + tip
    if (!tip.isFinite() || !total.isFinite()) return emptyList()
    return listOf(context.getString(R.string.working_tip, n(bill), n(rate), n(tip)),
        context.getString(R.string.working_total, n(bill), n(tip), n(total)),
        context.getString(R.string.working_split, n(total), people, n(total / people)))
}

internal fun discountWorking(context: Context, original: Double, rates: List<Double>): List<String> {
    if (!original.isFinite() || original < 0 || rates.isEmpty() || rates.any { !it.isFinite() || it !in 0.0..100.0 }) return emptyList()
    var price = original
    val steps = rates.mapIndexed { index, rate ->
        val before = price
        price *= 1 - rate / 100
        context.getString(R.string.working_discount_step, index + 1, n(before), n(rate), n(price))
    }
    val saved = original - price
    return steps + context.getString(R.string.working_saved, n(original), n(price), n(saved)) +
        if (original == 0.0) listOf(context.getString(R.string.working_zero_price))
        else listOf(context.getString(R.string.working_effective, n(saved), n(original), n(saved / original * 100)))
}

internal fun taxWorking(context: Context, amount: Double, rate: Double, inclusive: Boolean): List<String> {
    val result = calculateGst(amount, rate, inclusive) ?: return emptyList()
    return if (inclusive) listOf(
        context.getString(R.string.working_tax_base, n(amount), n(rate), n(result.baseAmount)),
        context.getString(R.string.working_tax_difference, n(amount), n(result.baseAmount), n(result.taxAmount)))
    else listOf(context.getString(R.string.working_tax_added, n(amount), n(rate), n(result.taxAmount)),
        context.getString(R.string.working_total, n(amount), n(result.taxAmount), n(result.totalAmount)))
}

internal fun ratioWorking(context: Context, a: Double, b: Double): List<String> {
    if (!a.isFinite() || !b.isFinite() || a == 0.0 && b == 0.0) return emptyList()
    val x = BigDecimal.valueOf(a)
    val y = BigDecimal.valueOf(b)
    val scale = maxOf(x.scale(), y.scale())
    val first = x.movePointRight(scale).toBigIntegerExact()
    val second = y.movePointRight(scale).toBigIntegerExact()
    val gcd = first.abs().gcd(second.abs())
    return listOf(context.getString(R.string.working_ratio_scale, x.toPlainString(), y.toPlainString(), scale, first.toString(), second.toString()),
        context.getString(R.string.working_ratio_gcd, gcd.toString()),
        context.getString(R.string.working_ratio_divide, first.toString(), second.toString(), gcd.toString(), (first / gcd).toString(), (second / gcd).toString()),
        context.getString(R.string.working_ratio_precision))
}
