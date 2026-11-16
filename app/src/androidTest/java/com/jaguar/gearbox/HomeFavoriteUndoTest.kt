package com.jaguar.gearbox

import android.content.Context
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

    @Test fun undoRestoresOriginalPositionAndReAddCancelsStaleUndo() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val prefs = context.getSharedPreferences("home-undo-${UUID.randomUUID()}", Context.MODE_PRIVATE)
        val favorites = FavoritesStore(prefs)
        val original = listOf(Tools.ROUTE_BMI, Tools.ROUTE_AVERAGE, Tools.ROUTE_RATIOS)
        favorites.addAll(original)
        val tool = Tools.byRoute(Tools.ROUTE_AVERAGE)!!
        try {
            compose.setContent {
                MaterialTheme { HomeScreen(listOf(tool), favorites, RecentToolsStore(context), {}) }
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
