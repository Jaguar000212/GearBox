package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.io.File
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import com.jaguar.gearbox.data.FavoritesStore
import org.junit.After
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.jaguar.gearbox.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class ToolSwitchDraftTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val prefix = "switch_drafts_${UUID.randomUUID()}_"
    private val directory get() = File(compose.activity.cacheDir, prefix)
    private val preferenceNames = mutableSetOf<String>()
    private fun isolatedContext(): Context = object : ContextWrapper(compose.activity) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val isolatedName = prefix + name
            preferenceNames += isolatedName
            return compose.activity.getSharedPreferences(isolatedName, mode)
        }
    }
    @After fun cleanup() {
        directory.deleteRecursively()
        preferenceNames.forEach { compose.activity.deleteSharedPreferences(it) }
    }

    private fun switchTo(name: String) {
        compose.onNodeWithContentDescription("More tool actions").performClick()
        compose.onNodeWithText("Switch tool").performClick()
        compose.onNodeWithText("Find a tool").performTextInput(name)
        compose.onNode(hasText(name) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
    }

    @Test fun multipleSwitchesRestoreEachDraftAndBackLeavesTool() {
        val isolated = isolatedContext()
        val favorites = FavoritesStore(isolated.getSharedPreferences("favorites", Context.MODE_PRIVATE))
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
            var route by remember { mutableStateOf<String?>("tool/number_to_words") }
            GearBoxApp(ThemeMode.SYSTEM, {}, false, {}, false, {}, pendingRoute = route,
                onPendingRouteConsumed = { route = null }, favoritesStore = favorites)
            }
        }
        compose.onNodeWithText("Whole number").performTextReplacement("123")
        switchTo("Decimal to Fraction Converter")
        compose.onNodeWithText("Decimal number").performTextReplacement("0.25")
        switchTo("Number to Roman")
        compose.onNodeWithText("Number (1-3999)").performTextReplacement("42")
        switchTo("Number to Words")
        compose.onNodeWithText("Whole number").assertTextContains("123")
        switchTo("Decimal to Fraction Converter")
        compose.onNodeWithText("Decimal number").assertTextContains("0.25")
        pressBack()
        compose.onNode(hasText("Home") and isSelectable()).assertIsSelected()
    }
}
