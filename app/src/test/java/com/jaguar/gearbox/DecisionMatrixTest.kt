package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class DecisionMatrixTest {
    @Test fun weightedMeansUseExactNumeratorsAndCompetitionTies() {
        val criteria = listOf(DecisionCriterion("Value", 2), DecisionCriterion("Comfort", 1))
        val result = evaluateDecisionMatrix(listOf(DecisionOption("A", listOf(5, 2)),
            DecisionOption("B", listOf(4, 4)), DecisionOption("C", listOf(3, 5))), criteria)
        assertEquals(3, result.totalWeight)
        assertEquals(listOf(12, 12, 11), result.scores.map { it.numerator })
        assertEquals(listOf(1, 1, 3), result.scores.map { it.rank })
        assertEquals("4.0000", result.scores.first().displayMean())
        assertEquals("3.6667", result.scores.last().displayMean())
    }
    @Test fun zeroWeightHasNoEffectAndMaximumsAreBounded() {
        val result = evaluateDecisionMatrix(listOf(DecisionOption("A", listOf(2, 5)), DecisionOption("B", listOf(3, 0))),
            listOf(DecisionCriterion("Included", 100), DecisionCriterion("Ignored", 0)))
        assertEquals(1, result.scores.first().optionIndex)
        val maximum = evaluateDecisionMatrix(List(6) { DecisionOption("Option", List(6) { 5 }) }, List(6) { DecisionCriterion("Criterion", 100) })
        assertEquals(600, maximum.totalWeight)
        assertTrue(maximum.scores.all { it.numerator == 3000 && it.rank == 1 })
    }
    @Test fun labelConstraintsAreExplicit() {
        assertTrue(validDecisionLabel("a".repeat(60)))
        assertFalse(validDecisionLabel("a".repeat(61)))
        assertFalse(validDecisionLabel(" "))
        assertFalse(validDecisionLabel("a\nb"))
        assertFalse(validDecisionLabel("a\u2028b"))
        assertFalse(validDecisionLabel("a\u2029b"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsAllZeroWeight() {
        evaluateDecisionMatrix(listOf(DecisionOption("A", listOf(1)), DecisionOption("B", listOf(2))), listOf(DecisionCriterion("C", 0)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsRaggedScores() {
        evaluateDecisionMatrix(listOf(DecisionOption("A", emptyList()), DecisionOption("B", listOf(2))), listOf(DecisionCriterion("C", 1)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOutOfRangeRatings() {
        evaluateDecisionMatrix(listOf(DecisionOption("A", listOf(6)), DecisionOption("B", listOf(2))), listOf(DecisionCriterion("C", 1)))
    }
}
