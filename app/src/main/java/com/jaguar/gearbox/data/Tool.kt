package com.jaguar.gearbox.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes a single tool available in the app.
 *
 * In the original Java Tool-Kit each tool was a separate [android.app.Activity] and a [Tool]
 * held a [Class] reference to it. In the Compose version every tool is just a destination in the
 * navigation graph, identified by its [route] - and [content] is the screen itself, so
 * registering a new tool's navigation destination is a one-line addition to [Tools.all] instead
 * of touching a separate `NavHost` registration for every tool.
 */
data class Tool(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val category: ToolCategory,
    val content: @Composable (onNavigateBack: () -> Unit) -> Unit,
)

/** Groups tools shown on the home screen so similar tools appear together. */
enum class ToolCategory(val label: String) {
    CALCULATORS("Calculators"),
    CONVERTERS("Converters"),
    GAMES_AND_RANDOM("Games & Randomizers"),
    UTILITIES("Utilities"),
}
