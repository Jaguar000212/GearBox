package com.jaguar.gearbox.ui.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class RpsRulesTest {
    @Test fun allNineMatchups() {
        val expected = listOf(
            listOf(RpsOutcome.DRAW, RpsOutcome.LOSS, RpsOutcome.WIN),
            listOf(RpsOutcome.WIN, RpsOutcome.DRAW, RpsOutcome.LOSS),
            listOf(RpsOutcome.LOSS, RpsOutcome.WIN, RpsOutcome.DRAW),
        )
        RpsMove.entries.forEachIndexed { p, player ->
            RpsMove.entries.forEachIndexed { c, computer ->
                assertEquals("$player vs $computer", expected[p][c], rpsOutcome(player, computer))
            }
        }
    }
}
