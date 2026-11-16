package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RandomLastResultRestoreTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private fun seed(route: String, inputs: Map<String, String>) {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(rememberResults = true, historyEnabled = true))
        assertTrue(store.saveResult(route, inputs, "Deliberately not parsed display text"))
        assertTrue(store.clearHistory(route))
    }
    private fun show(content: @Composable () -> Unit) {
        compose.setContent { isolated.Provide { MaterialTheme { content() } } }
    }
    private fun expect(text: String, route: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        Thread.sleep(300)
        assertTrue(ToolMemoryStore(isolated).history(route).isEmpty())
    }
    @Test fun coinHydratesExactOutcomeWithoutFlipOrHistoryReplay() {
        seed(Tools.ROUTE_FLIP_COIN, mapOf("coin" to "Fair coin", "_run" to "old", "_heads" to "false"))
        show { FlipCoinScreen({}) }; expect("Tails", Tools.ROUTE_FLIP_COIN)
    }
    @Test fun bottleHydratesExactAngleWithoutSpin() {
        seed(Tools.ROUTE_SPIN_BOTTLE, mapOf("spin" to "Full circle", "_run" to "old", "_angle" to "271"))
        show { SpinBottleScreen({}) }; expect("Bottle points 271° clockwise from the top", Tools.ROUTE_SPIN_BOTTLE)
    }
    @Test fun diceHydratesFacesWithoutRoll() {
        seed(Tools.ROUTE_DICE, mapOf("dice" to "2", "_run" to "old", "_dice" to "2,5"))
        show { DiceRollScreen({}) }; expect("Total: 7", Tools.ROUTE_DICE)
    }
    @Test fun randomNumberHydratesWithoutGenerate() {
        seed(Tools.ROUTE_RANDOM, mapOf("min" to "1", "max" to "20", "_run" to "old", "_number" to "17"))
        show { RandomNumberScreen({}) }; expect("17", Tools.ROUTE_RANDOM)
    }
    @Test fun sameLabelChoiceRetainsSlotWithoutChoosingAgain() {
        seed(Tools.ROUTE_ONE_OF_TWO, mapOf("optionA" to "Same", "optionB" to "Same", "_run" to "old", "_choice" to "1"))
        show { OneOfTwoScreen({}) }; expect("Option 2 selected\nSame", Tools.ROUTE_ONE_OF_TWO)
    }
    @Test fun rpsHydratesWithoutChangingTallies() {
        val prefs = isolated.getSharedPreferences("gearbox_prefs", 0)
        prefs.edit().putInt("rps.wins", 7).commit()
        seed(Tools.ROUTE_RPS, mapOf("playerMove" to "ROCK", "_run" to "old", "_cpuMove" to "SCISSORS"))
        show { RockPaperScissorsScreen({}) }; expect("You win!", Tools.ROUTE_RPS)
        assertEquals(7, prefs.getInt("rps.wins", 0))
    }
    @Test fun hydratedResultIsNotNewHistoryButSubsequentEditIs() {
        val route = Tools.ROUTE_TEXT_TOOLS
        seed(route, mapOf("text" to "saved"))
        val input = mutableStateOf("")
        show {
            rememberToolHistory(route, mapOf("text" to input.value), input.value.takeIf { it.isNotEmpty() },
                onRestore = { input.value = it["text"].orEmpty() }, recordingDelayMillis = 0L)
        }
        compose.waitUntil(5_000) { input.value == "saved" }
        compose.waitForIdle(); Thread.sleep(300)
        val store = ToolMemoryStore(isolated)
        assertTrue(store.history(route).isEmpty())
        compose.runOnIdle { input.value = "edited" }
        compose.waitUntil(5_000) { store.history(route).singleOrNull()?.output == "edited" }
    }
    @Test fun inputOnlyHydrationDoesNotSuppressExplicitGenerate() {
        val route = Tools.ROUTE_HASH_GENERATOR
        seed(route, mapOf("text" to "saved"))
        val input = mutableStateOf("")
        val generated = mutableStateOf(false)
        show {
            rememberToolHistory(route, mapOf("text" to input.value),
                if (generated.value) "new digest" else null,
                onRestore = { input.value = it["text"].orEmpty() },
                resultRequiresExplicitAction = true, recordingDelayMillis = 0L)
        }
        compose.waitUntil(5_000) { input.value == "saved" }
        compose.runOnIdle { generated.value = true }
        compose.waitUntil(5_000) { ToolMemoryStore(isolated).history(route).singleOrNull()?.output == "new digest" }
    }

    @Test fun asyncHydrationNullToOutputDoesNotRepopulateHistory() {
        val route = Tools.ROUTE_TEXT_TOOLS
        seed(route, mapOf("text" to "saved"))
        val input = mutableStateOf("")
        val complete = mutableStateOf(false)
        show {
            rememberToolHistory(route, mapOf("text" to input.value),
                if (complete.value) "computed" else null,
                onRestore = { input.value = it["text"].orEmpty() }, recordingDelayMillis = 0L)
        }
        compose.waitUntil(5_000) { input.value == "saved" }
        compose.runOnIdle { complete.value = true }
        compose.waitForIdle(); Thread.sleep(300)
        assertTrue(ToolMemoryStore(isolated).history(route).isEmpty())
    }

    @Test fun repeatedExplicitRpsImportsRestoreOutcomeWithoutIncrementingTally() {
        val prefs = isolated.getSharedPreferences("gearbox_prefs", 0)
        prefs.edit().putInt("rps.wins", 7).commit()
        val pending = mutableStateOf<ToolImport?>(null)
        show {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) {
                RockPaperScissorsScreen({})
            }
        }
        compose.waitForIdle()
        repeat(2) {
            compose.runOnIdle {
                pending.value = ToolImport(Tools.ROUTE_RPS,
                    mapOf("playerMove" to "ROCK", "_cpuMove" to "SCISSORS", "_run" to "old"))
            }
            compose.waitUntil(5_000) { pending.value == null }
            compose.onNodeWithText("You win!").assertExists()
            assertEquals(7, prefs.getInt("rps.wins", 0))
        }
        compose.runOnIdle {
            pending.value = ToolImport(Tools.ROUTE_RPS,
                mapOf("playerMove" to "ROCK", "_cpuMove" to "INVALID"))
        }
        compose.waitUntil(5_000) { pending.value == null }
        compose.onNodeWithText("Previous choice: Rock. Tap a move to play.").assertExists()
        assertEquals(7, prefs.getInt("rps.wins", 0))
    }

    @Test fun legacyCoinRecordDoesNotInventAnOutcome() {
        seed(Tools.ROUTE_FLIP_COIN, mapOf("coin" to "Fair coin", "_run" to "old"))
        show { FlipCoinScreen({}) }
        compose.waitForIdle()
        compose.onNodeWithText("Ready to flip").assertExists()
        assertTrue(ToolMemoryStore(isolated).history(Tools.ROUTE_FLIP_COIN).isEmpty())
    }

    @Test fun resultHydrationDoesNotResurrectClearedModesButChangingModeSaves() {
        val route = Tools.ROUTE_DICE
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(rememberModes = true, rememberResults = true))
        store.saveResult(route, mapOf("dice" to "2"), "old result")
        store.clearRememberedModes()
        val count = mutableStateOf("1")
        show {
            rememberToolHistory(route, mapOf("dice" to count.value), null,
                onRestore = { count.value = it["dice"].orEmpty() },
                modeInputs = mapOf("dice" to count.value),
                onRestoreMode = { count.value = it["dice"].orEmpty() })
        }
        compose.waitUntil(5_000) { count.value == "2" }
        compose.waitForIdle(); Thread.sleep(300)
        assertTrue(store.loadMode(route).isEmpty())
        compose.runOnIdle { count.value = "3" }
        compose.waitUntil(5_000) { store.loadMode(route)["dice"] == "3" }
    }

}
