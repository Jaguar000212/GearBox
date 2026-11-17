package com.jaguar.gearbox.logic

import java.math.BigDecimal
import java.math.RoundingMode

data class DecisionOption(val label: String, val ratings: List<Int>)
data class DecisionCriterion(val label: String, val weight: Int)
data class DecisionScore(val optionIndex: Int, val numerator: Int, val denominator: Int, val rank: Int) {
    fun displayMean(): String = BigDecimal(numerator).divide(BigDecimal(denominator), 4, RoundingMode.HALF_UP).toPlainString()
}
data class DecisionMatrixResult(val totalWeight: Int, val scores: List<DecisionScore>)
fun validDecisionLabel(text: String): Boolean = text.isNotBlank() && text.length <= 60 && text.none { it.isISOControl() || it == '\u2028' || it == '\u2029' }
/** All ratings mean higher is better. Rank exact integer numerators, never rounded display means. */
fun evaluateDecisionMatrix(options: List<DecisionOption>, criteria: List<DecisionCriterion>): DecisionMatrixResult {
    require(options.size in 2..6 && criteria.size in 1..6)
    require(options.all { validDecisionLabel(it.label) && it.ratings.size == criteria.size && it.ratings.all { rating -> rating in 0..5 } })
    require(criteria.all { validDecisionLabel(it.label) && it.weight in 0..100 })
    val total = criteria.sumOf { it.weight }
    require(total > 0)
    val numerators = options.map { option -> criteria.indices.sumOf { option.ratings[it] * criteria[it].weight } }
    return DecisionMatrixResult(total, numerators.mapIndexed { index, numerator ->
        DecisionScore(index, numerator, total, 1 + numerators.count { it > numerator })
    }.sortedWith(compareBy<DecisionScore> { it.rank }.thenBy { it.optionIndex }))
}
