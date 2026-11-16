package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID

/** Keeps screen tests independent of installed-app memory, preferences and launcher shortcuts. */
internal class IsolatedToolTestContext : ContextWrapper(
    InstrumentationRegistry.getInstrumentation().targetContext,
) {
    private val storage = InstrumentationRegistry.getInstrumentation().context
    private val prefix = "isolated_tool_${UUID.randomUUID()}_"
    private val directory = File(storage.cacheDir, prefix)
    private val preferenceNames = mutableSetOf<String>()

    override fun getApplicationContext(): Context = this
    override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        val isolatedName = prefix + name
        preferenceNames += isolatedName
        return storage.getSharedPreferences(isolatedName, mode)
    }
    override fun getSystemService(name: String): Any? =
        if (name == Context.SHORTCUT_SERVICE) null else super.getSystemService(name)

    @Composable fun Provide(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalContext provides this, content = content)
    }

    fun cleanup() {
        directory.deleteRecursively()
        preferenceNames.forEach { storage.deleteSharedPreferences(it) }
    }
}
