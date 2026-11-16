package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.ToolCategory
import com.jaguar.gearbox.data.localizedLabel

/** Stable visual identity, independent of translated labels and persisted category IDs. */
internal fun categoryIcon(category: ToolCategory): ImageVector = when (category) {
    ToolCategory.CALCULATORS -> Icons.Default.Calculate
    ToolCategory.MONEY -> Icons.Default.Payments
    ToolCategory.TIME -> Icons.Default.Schedule
    ToolCategory.UTILITIES -> Icons.Default.TextFields
    ToolCategory.GAMES_AND_RANDOM -> Icons.Default.SportsEsports
    ToolCategory.RANDOM -> Icons.Default.Shuffle
    ToolCategory.DEVICE -> Icons.Default.Straighten
}

private data class CategoryColors(val background: Color, val foreground: Color)

@Composable
private fun categoryColors(category: ToolCategory): CategoryColors {
    val scheme = MaterialTheme.colorScheme
    // Theme roles keep light, dark and dynamic-color palettes coherent. Icons and labels, not
    // colors, distinguish every category; similar theme hues are intentionally acceptable.
    val (container, foreground) = when (category) {
        ToolCategory.CALCULATORS, ToolCategory.UTILITIES -> scheme.primaryContainer to scheme.onPrimaryContainer
        ToolCategory.MONEY, ToolCategory.DEVICE -> scheme.secondaryContainer to scheme.onSecondaryContainer
        ToolCategory.TIME, ToolCategory.RANDOM -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        ToolCategory.GAMES_AND_RANDOM -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    return CategoryColors(lerp(scheme.surface, container, 0.8f), foreground)
}

/** Decorative alongside the always-visible localized category text. */
@Composable
fun CategoryEmblem(category: ToolCategory, modifier: Modifier = Modifier) {
    val colors = categoryColors(category)
    Surface(modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = colors.background) {
        Box(contentAlignment = Alignment.Center) {
            Icon(categoryIcon(category), contentDescription = null, tint = colors.foreground, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun CategoryHeading(category: ToolCategory, count: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryEmblem(category)
        Column(Modifier.weight(1f)) {
            Text(category.localizedLabel(context), style = MaterialTheme.typography.titleMedium)
            Text(pluralStringResource(R.plurals.screen_tool_count, count, count),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
