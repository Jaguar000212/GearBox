package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import java.io.File
import org.junit.After
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.screens.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class HomeFavoriteUndoTest {
    @get:Rule val compose = createComposeRule()

    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefix = "home_undo_${UUID.randomUUID()}_"
    private val directory = File(base.cacheDir, prefix)
    private val preferenceNames = mutableSetOf<String>()
    private val context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val isolatedName = prefix + name
            preferenceNames += isolatedName
            return base.getSharedPreferences(isolatedName, mode)
        }
    }
    @After fun cleanup() {
        directory.deleteRecursively()
        preferenceNames.forEach { base.deleteSharedPreferences(it) }
    }

    @Test fun undoRestoresOriginalPositionAndReAddCancelsStaleUndo() {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        val recents = RecentToolsStore(context)
        val favorites = FavoritesStore(prefs)
        val original = listOf(Tools.ROUTE_BMI, Tools.ROUTE_AVERAGE, Tools.ROUTE_RATIOS)
        favorites.addAll(original)
        val tool = Tools.byRoute(Tools.ROUTE_AVERAGE)!!
        try {
            compose.setContent {
                CompositionLocalProvider(LocalContext provides context) {
                    MaterialTheme { HomeScreen(listOf(tool), favorites, recents, {}) }
                }
            }
            compose.onNodeWithContentDescription("Remove Average Calculator from favorites").performClick()
            compose.onNodeWithText("Undo").performClick()
            compose.runOnIdle { assertEquals(original, favorites.favorites.toList()) }
            compose.onNodeWithContentDescription("Remove Average Calculator from favorites").performClick()
            compose.onNodeWithContentDescription("Add Average Calculator to favorites").performClick()
            compose.onNodeWithText("Undo").assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(listOf(Tools.ROUTE_BMI, Tools.ROUTE_RATIOS, Tools.ROUTE_AVERAGE), favorites.favorites.toList())
            }
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
