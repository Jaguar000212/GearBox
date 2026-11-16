package com.jaguar.gearbox

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.ui.tools.clockDurationDescription
import com.jaguar.gearbox.ui.tools.stopwatchResultText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Read-only resource checks: no clock storage, notification, or clipboard mutation. */
class GameClockStringsTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext.let {
        it.createConfigurationContext(Configuration(it.resources.configuration).apply { setLocale(Locale.ENGLISH) })
    }

    @Test fun stopwatchExportPreservesEnglishAndLaps() {
        assertEquals("Stopwatch: 00:05.00\nLap 1: split 00:02.00, total 00:02.00\nLap 2: split 00:03.00, total 00:05.00",
            stopwatchResultText(5000L, listOf(2000L, 5000L), context))
    }

    @Test fun diceNounsAndFormattedResultsPreserveEnglish() {
        assertEquals("die", context.resources.getQuantityString(R.plurals.game_dice_noun, 1))
        assertEquals("dice", context.resources.getQuantityString(R.plurals.game_dice_noun, 2))
        assertEquals("Dice Roll (2 six-sided dice): 2, 4\nTotal: 6",
            context.getString(R.string.game_dice_roll_1_s_six_sided_2_s_3_s_ntotal_4_s, 2, "dice", "2, 4", 6))
    }

    @Test fun leadingSpaceAndApostropheSurviveResourceEscaping() {
        assertEquals("  Reset", context.getString(R.string.game_reset))
        assertEquals("Time's up!", context.getString(R.string.game_time_s_up))
    }
    @Test fun durationAccessibilityUsesSingularAndPluralUnits() {
        assertEquals("1 hour, 1 minute, 1 second", clockDurationDescription(context, 3661L))
        assertEquals("0 hours, 2 minutes, 0 seconds", clockDurationDescription(context, 120L))
        assertEquals("2 hours, 0 minutes, 2 seconds", clockDurationDescription(context, 7202L))
    }
}
