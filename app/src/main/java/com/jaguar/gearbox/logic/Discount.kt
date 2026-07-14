package com.jaguar.gearbox.logic

/**
 * Applies each discount percentage in [discountPercents] sequentially to [originalPrice] - e.g.
 * 20% then 10% off ₹100 is ₹80 then ₹72, not a flat 30% off (₹70). This is how "stacked" discounts
 * actually compound in retail pricing.
 */
data class DiscountResult(
    val finalPrice: Double,
    val totalSaved: Double,
    val effectivePercent: Double
)

fun applyStackedDiscounts(originalPrice: Double, discountPercents: List<Double>): DiscountResult {
    val finalPrice = discountPercents.fold(originalPrice) { price, percent ->
        price * (1 - percent / 100.0)
    }
    val saved = originalPrice - finalPrice
    val effectivePercent = if (originalPrice == 0.0) 0.0 else saved / originalPrice * 100.0
    return DiscountResult(finalPrice, saved, effectivePercent)
}
