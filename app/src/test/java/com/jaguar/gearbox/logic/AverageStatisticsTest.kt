package com.jaguar.gearbox.logic

import org.junit.Assert.*
import org.junit.Test

class AverageStatisticsTest {
    @Test fun structuredResultsPreserveLegacyExport() {
        val result = computeAverageStatistics("1,2,3,4,5")!!
        assertEquals("3", result.arithmetic)
        assertEquals("15", result.sum)
        assertEquals(5, result.count)
        assertEquals("1", result.smallest)
        assertEquals("5", result.largest)
        assertEquals(computeAverageResult("1,2,3,4,5"), result.asText())
    }
    @Test fun unsupportedMeansRetainExplanations() {
        assertTrue(computeAverageStatistics("0,1")!!.harmonic.contains("zero"))
        assertTrue(computeAverageStatistics("-1,1")!!.geometric.contains("negative"))
    }
    @Test fun invalidInputDoesNotProduceResults() {
        listOf("", "hello", "1,NaN", "Infinity", "1e308,1e308").forEach {
            assertNull(computeAverageStatistics(it))
        }
    }
}
