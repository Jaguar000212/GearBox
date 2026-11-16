package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import org.junit.After
import java.io.File
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jaguar.gearbox.ui.components.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToolImportPrecedenceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private var isolatedDirectory: File? = null
    @After fun cleanup() { isolatedDirectory?.deleteRecursively() }

    @Test fun newImportsApplyOnceEvenAfterInitializationAndModesLeaveInputsAlone() {
        val directory = File(compose.activity.cacheDir, "imports_${UUID.randomUUID()}").also { isolatedDirectory = it }
        val context = object : ContextWrapper(compose.activity) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
        }
        val pending = mutableStateOf<ToolImport?>(null)
        var applied = 0
        compose.setContent {
            var input by remember { mutableStateOf("draft") }
            var mode by remember { mutableStateOf("original") }
            CompositionLocalProvider(LocalContext provides context, LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) {
                rememberToolHistory("tool/text_tools", mapOf("text" to input), null,
                    onRestore = { input = it["text"].orEmpty(); applied++ },
                    onRestoreMode = { mode = it["mode"].orEmpty(); applied++ },
                    restoreLastResult = false)
                Text("$input|$mode")
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { applied = 0; pending.value = ToolImport("tool/text_tools", mapOf("text" to "imported")) }
        compose.onNodeWithText("imported|original").assertExists()
        compose.runOnIdle { assertEquals(1, applied); pending.value = ToolImport("tool/text_tools", mapOf("mode" to "new"), modeOnly = true) }
        compose.onNodeWithText("imported|new").assertExists()
        compose.runOnIdle { assertEquals(2, applied); pending.value = ToolImport("tool/text_tools", mapOf("text" to "imported")) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(3, applied) }
    }
}
