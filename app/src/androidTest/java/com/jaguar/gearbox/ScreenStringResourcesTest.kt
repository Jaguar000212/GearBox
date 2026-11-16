package com.jaguar.gearbox

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

class ScreenStringResourcesTest {
    private fun englishContext() = InstrumentationRegistry.getInstrumentation().targetContext.let { base ->
        base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(Locale.ENGLISH) })
    }

    @Test fun screenFormatsPreserveNamesAndReadableActions() {
        val context = englishContext()
        assertEquals("Move Counter up", context.getString(R.string.screen_move_1_s_up, "Counter"))
        assertEquals("Text Tools: automatic history", context.getString(R.string.screen_1_s_automatic_history, "Text Tools"))
        assertEquals("Visible · Collection paused", context.getString(R.string.screen_recent_status,
            context.getString(R.string.screen_visible), context.getString(R.string.screen_collection_paused)))
        assertFalse(context.getString(R.string.screen_find_counter_and_timer_stopwatch_on_your_launcher).contains("\${"))
    }

    @Test fun countsUseSingularAndPluralResources() {
        val resources = englishContext().resources
        assertEquals("1 tool", resources.getQuantityString(R.plurals.screen_tool_count, 1, 1))
        assertEquals("41 tools", resources.getQuantityString(R.plurals.screen_tool_count, 41, 41))
        assertEquals("1 history entry", resources.getQuantityString(R.plurals.screen_history_count, 1, 1))
        assertEquals("2 history entries", resources.getQuantityString(R.plurals.screen_history_count, 2, 2))
        assertEquals("1 remembered result", resources.getQuantityString(R.plurals.screen_result_count, 1, 1))
    }
}
