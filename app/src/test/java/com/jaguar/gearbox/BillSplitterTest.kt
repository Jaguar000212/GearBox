package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class BillSplitterTest {
    private fun split(bill: String, tax: String = "0", tip: String = "0", diners: Int = 3) =
        calculateBillSplit(BigDecimal(bill), BigDecimal(tax), BigDecimal(tip), diners)
    @Test fun exactRemainderConservation() {
        for (diners in 1..1000) {
            val result = split("10.01", "0.99", "12.5", diners)
            assertEquals(result.totalMinor, result.baseShareMinor * diners + result.extraCentDiners)
            assertTrue(result.extraCentDiners in 0 until diners)
        }
    }
    @Test fun tipUsesPretaxBillAndHalfUpOnce() {
        val result = split("0.05", "100", "10", 2)
        assertEquals(1L, result.tipMinor)
        assertEquals(10006L, result.totalMinor)
        assertEquals(5003L, result.baseShareMinor)
    }
    @Test fun distributionIncludesZeroSharesWhenNeeded() {
        val result = split("0.01", diners = 1000)
        assertEquals(0L, result.baseShareMinor)
        assertEquals(1, result.extraCentDiners)
        assertEquals(0L, split("0").totalMinor)
    }
    @Test fun maximumsStayExact() {
        val result = split("1000000000", "1000000000", "100", 1000)
        assertEquals(300000000000L, result.totalMinor)
        assertEquals("3000000.00", billMinorText(result.baseShareMinor))
    }
    @Test fun invalidAmountsAndPercentagesRejected() {
        for (text in listOf("", "-1", "+1", "1e2", "1,000", "01", ".5", "1.", "0.001", "1000000000.01", " 1", "NaN")) assertNull(text, parseBillAmount(text))
        assertNotNull(parseBillAmount("1000000000.00"))
        assertNull(parseBillPercent("100.01"))
        assertNotNull(parseBillPercent("99.99"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNoDiners() { split("1", diners = 0) }
    @Test(expected = IllegalArgumentException::class) fun rejectsTooManyDiners() { split("1", diners = 1001) }
}
