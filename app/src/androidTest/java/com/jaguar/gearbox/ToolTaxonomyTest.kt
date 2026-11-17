package com.jaguar.gearbox

import com.jaguar.gearbox.data.ToolCategory
import com.jaguar.gearbox.data.Tools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolTaxonomyTest {
    @Test fun everyToolHasOneHomeAndRoutesStayUnique() {
        assertEquals(50, Tools.all.size)
        assertEquals(50, Tools.all.map { it.route }.distinct().size)
        assertEquals(7, ToolCategory.entries.size)
        val expected = mapOf(
            ToolCategory.CALCULATORS to 11, ToolCategory.MONEY to 6,
            ToolCategory.TIME to 8, ToolCategory.UTILITIES to 9,
            ToolCategory.GAMES_AND_RANDOM to 7, ToolCategory.RANDOM to 4,
            ToolCategory.DEVICE to 5,
        )
        assertEquals(expected, Tools.all.groupingBy { it.category }.eachCount())
        assertTrue(Tools.all.filter { it.name.contains("Scoreboard") }
            .all { it.category == ToolCategory.GAMES_AND_RANDOM })
    }
}
