package com.jaguar.gearbox.data

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes a single tool available in the app.
 *
 * In the original Java Tool-Kit each tool was a separate [android.app.Activity] and a [Tool]
 * held a [Class] reference to it. In the Compose version every tool is just a destination in the
 * navigation graph, so a tool is identified by its [route].
 */
data class Tool(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
)
