package com.jaguar.gearbox.logic

/**
 * GST/sales-tax math for both directions: [inclusive] = true means [amount] already has tax
 * baked in (back out the base + tax from it), false means [amount] is the pre-tax base (add tax
 * on top). Same formula shape as any percentage-inclusive-of-tax calculation, not GST-specific.
 */
data class GstResult(val baseAmount: Double, val taxAmount: Double, val totalAmount: Double)

fun calculateGst(amount: Double, ratePercent: Double, inclusive: Boolean): GstResult {
    return if (inclusive) {
        val base = amount / (1 + ratePercent / 100.0)
        GstResult(baseAmount = base, taxAmount = amount - base, totalAmount = amount)
    } else {
        val tax = amount * ratePercent / 100.0
        GstResult(baseAmount = amount, taxAmount = tax, totalAmount = amount + tax)
    }
}
