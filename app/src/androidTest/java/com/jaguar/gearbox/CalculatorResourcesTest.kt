package com.jaguar.gearbox

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.logic.computeAverageStatistics
import com.jaguar.gearbox.logic.computeLoanResult
import com.jaguar.gearbox.ui.tools.localizedAverageResult
import com.jaguar.gearbox.ui.tools.localizedBmiCategory
import com.jaguar.gearbox.ui.tools.localizedDaysUntilSummary
import com.jaguar.gearbox.ui.tools.localizedLoanResult
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorResourcesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.let {
        it.createConfigurationContext(Configuration(it.resources.configuration).apply { setLocale(Locale.ENGLISH) })
    }

    @Test fun englishAverageAndLoanAdaptersPreserveExistingExports() {
        listOf("1, 2, 3", "0, 1", "-1, 1").forEach { input ->
            val stats = computeAverageStatistics(input)!!
            assertEquals(stats.asText().replaceFirst("Result:", "Arithmetic mean:"), localizedAverageResult(context, stats))
        }
        val loan = computeLoanResult(100000.0, 6.0, 5.0)!!
        assertEquals(loan, localizedLoanResult(context, loan))
        assertEquals(loan.replace("Interest Percentage:", "Interest share of total repayment:"),
            localizedLoanResult(context, loan, interestShareLabel = true))
        assertEquals("Note: Interest Percentage: is not a result label here",
            localizedLoanResult(context, "Note: Interest Percentage: is not a result label here", interestShareLabel = true))
        listOf("Underweight", "Normal weight", "Overweight", "Obese").forEach {
            assertEquals(it, localizedBmiCategory(context, it))
        }
    }

    @Test fun dayCountsUsePluralResourcesAndKeepLargeCounts() {
        val today = LocalDate.of(2026, 1, 1)
        assertEquals("1 day until the date", localizedDaysUntilSummary(context, today, today.plusDays(1), ""))
        assertEquals("2 days since Launch", localizedDaysUntilSummary(context, today, today.minusDays(2), "Launch"))
        assertEquals("Launch is today!", localizedDaysUntilSummary(context, today, today, "Launch"))
        assertEquals("3000000000 days until the date", localizedDaysUntilSummary(context, today, today.plusDays(3000000000L), ""))
    }

    @Test fun formattedResourcesKeepPercentSignsAndLeadingSpaces() {
        assertEquals("20% of 100 = 20", context.getString(R.string.calc_1_s_of_2_s_3_s, "20", "100", "20"))
        assertEquals(" Clear", context.getString(R.string.calc_clear))
    }
}
