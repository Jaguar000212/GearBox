package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.IncomingTextDestination
import com.jaguar.gearbox.data.ToolAutomaticMemory
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.LocalConsumeToolImport
import com.jaguar.gearbox.ui.components.LocalToolImport
import com.jaguar.gearbox.ui.components.ToolImport
import com.jaguar.gearbox.ui.tools.HashGeneratorScreen
import com.jaguar.gearbox.ui.tools.TextToolsScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real screen/history path, isolated from installed-app preferences and private files. */
class IncomingRetentionTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    private val store = ToolMemoryStore(isolated)
    @After fun cleanup() { isolated.cleanup() }

    private fun enableAutomaticMemory(route: String) {
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true,
            automaticMemory = mapOf(route to ToolAutomaticMemory(historyEnabled = true, rememberResults = true)))))
        assertTrue(store.settings().permitsHistory(route))
        assertTrue(store.settings().permitsResult(route))
    }

    /** Both Compose time and real IO time pass beyond the screen's 1-second recording debounce. */
    private fun assertNoAutomaticSaveAfterDebounce(route: String) {
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        compose.waitForIdle()
        assertTrue("Initial imported input must not enter history", store.history(route).isEmpty())
        assertNull("Initial imported input must not become the remembered result", store.loadResult(route))
    }

    @Test fun incomingTextStaysUnsavedAcrossStateRestorationUntilIntentionalEdit() {
        val route = Tools.ROUTE_TEXT_TOOLS
        enableAutomaticMemory(route)
        val incoming = "shared\ntext"
        val pending = mutableStateOf<ToolImport?>(ToolImport(route, requireNotNull(IncomingTextDestination.TEXT.inputs(incoming)), suppressInitialRetention = true))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TextToolsScreen({}) }
        } } }
        compose.onNode(hasText("Text") and hasSetTextAction()).assertTextContains(incoming)
        assertNoAutomaticSaveAfterDebounce(route)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasText("Text") and hasSetTextAction()).assertTextContains(incoming)
        assertNoAutomaticSaveAfterDebounce(route)
        compose.onNode(hasText("Text") and hasSetTextAction()).performTextReplacement("intentionally edited")
        compose.waitUntil(5_000) {
            store.history(route).any { it.inputs["text"] == "intentionally edited" } &&
                store.loadResult(route)?.inputs?.get("text") == "intentionally edited"
        }
        assertFalse(store.history(route).any { it.inputs["text"] == incoming })
    }

    @Test fun incomingHashRequiresGenerateAndThenCanBeRetained() {
        val route = Tools.ROUTE_HASH_GENERATOR
        enableAutomaticMemory(route)
        val incoming = "shared hash input"
        val pending = mutableStateOf<ToolImport?>(ToolImport(route, requireNotNull(IncomingTextDestination.HASH.inputs(incoming)), suppressInitialRetention = true))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { HashGeneratorScreen({}) }
        } } }
        compose.onNodeWithText("Generate hash").assertExists()
        assertNoAutomaticSaveAfterDebounce(route)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasText("Text") and hasSetTextAction()).assertTextContains(incoming)
        compose.onNodeWithText("Generate hash").assertExists()
        assertNoAutomaticSaveAfterDebounce(route)
        compose.onNodeWithText("Generate hash").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            store.history(route).any { it.inputs["text"] == incoming && it.output.isNotBlank() } &&
                store.loadResult(route)?.inputs?.get("text") == incoming
        }
    }

    @Test fun ordinaryExplicitTextImportsKeepExistingRetentionBehavior() {
        val route = Tools.ROUTE_TEXT_TOOLS
        enableAutomaticMemory(route)
        val pending = mutableStateOf<ToolImport?>(ToolImport(route, mapOf("text" to "ordinary import")))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TextToolsScreen({}) }
        } } }
        compose.waitUntil(5_000) {
            store.history(route).any { it.inputs["text"] == "ordinary import" } &&
                store.loadResult(route)?.inputs?.get("text") == "ordinary import"
        }
    }
}
