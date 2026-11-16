package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.TextHashAlgorithm
import com.jaguar.gearbox.logic.textHashHex
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.*
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DirectShareReviewTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    private val directory = File(isolated.noBackupFilesDir, "share_test_${UUID.randomUUID()}")
    private val sent = mutableListOf<Intent>()
    private val context = object : ContextWrapper(isolated) {
        override fun getApplicationContext(): Context = this
        override fun getCacheDir(): File = directory.also { it.mkdirs() }
        override fun startActivity(intent: Intent) { sent += intent }
    }
    @After fun cleanup() { isolated.cleanup() }
    private val pending = mutableStateOf<ToolImport?>(null)
    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) {
                MaterialTheme { content() }
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun sharedText(): String? = sent.single().getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.getStringExtra(Intent.EXTRA_TEXT)

    @Test fun textShareReviewsExactPayloadAndCancelSendsNothing() {
        show { TextToolsScreen({}) }
        compose.onNode(hasSetTextAction()).performTextInput("hello")
        compose.onNodeWithText("Share", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Review before sharing").assertExists()
        assertTrue(sent.isEmpty())
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(sent.isEmpty())
        compose.onNodeWithText("Share", substring = false).performClick()
        compose.onNodeWithText("Continue to share").performClick()
        assertEquals("Hello", sharedText())
    }

    @Test fun hashShareReviewsDigestNotOriginalInput() {
        show { HashGeneratorScreen({}) }
        compose.onNode(hasSetTextAction()).performTextInput("private source")
        compose.waitUntil(5_000) { compose.onAllNodesWithText(textHashHex("private source", TextHashAlgorithm.SHA256)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Share", substring = false).performScrollTo().performClick()
        assertTrue(sent.isEmpty())
        compose.onNodeWithText("Continue to share").performClick()
        assertEquals(textHashHex("private source", TextHashAlgorithm.SHA256), sharedText())
    }

    @Test fun changedTextInvalidatesPendingShare() {
        show { TextToolsScreen({}) }
        compose.onNode(hasSetTextAction()).performTextInput("old")
        compose.onNodeWithText("Share", substring = false).performScrollTo().performClick()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_TEXT_TOOLS, mapOf("text" to "new")) }
        compose.waitUntil(5_000) { pending.value == null }
        compose.onNodeWithText("Review before sharing").assertDoesNotExist()
        assertTrue(sent.isEmpty())
    }

    @Test fun qrReviewAndCancellationDoNotWriteImageOrLaunchShare() {
        show { QrGeneratorScreen({}) }
        compose.onNode(hasSetTextAction()).performTextInput("private QR content")
        compose.waitUntil(5_000) { compose.onAllNodesWithText(" Share QR image").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(" Share QR image").performScrollTo().performClick()
        compose.onNodeWithText("Content encoded in the QR image").assertExists()
        assertFalse(File(directory, "qr").exists())
        assertTrue(sent.isEmpty())
        compose.onNodeWithText("Cancel").performClick()
        assertFalse(File(directory, "qr").exists())
        assertTrue(sent.isEmpty())
        compose.onNodeWithText(" Share QR image").performClick()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_QR_GENERATOR, mapOf("text" to "updated")) }
        compose.waitUntil(5_000) { pending.value == null }
        compose.onNodeWithText("Review before sharing").assertDoesNotExist()
        assertFalse(File(directory, "qr").exists())
        assertTrue(sent.isEmpty())
    }
}
